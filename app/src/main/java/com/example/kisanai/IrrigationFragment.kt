package com.example.kisanai

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class IrrigationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: IrrigationAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutNoHistory: View
    private lateinit var btnClearAll: View
    private var currentHistory: List<IrrigationItem> = emptyList()
    private var historyListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_irrigation, container, false)
        
        recyclerView = view.findViewById(R.id.rvIrrigationHistory)
        progressBar = view.findViewById(R.id.pbIrrigation)
        layoutNoHistory = view.findViewById(R.id.layoutNoIrrigationHistory)
        btnClearAll = view.findViewById(R.id.btnClearAllIrrigation)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = IrrigationAdapter(emptyList()) { item ->
            showDeleteConfirmation(item)
        }
        recyclerView.adapter = adapter

        btnClearAll.setOnClickListener {
            showClearAllConfirmation()
        }

        fetchHistory()

        return view
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_history_title)
            .setMessage(R.string.clear_irrigation_history_msg)
            .setPositiveButton(R.string.clear_all) { _, _ ->
                clearAllHistory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun clearAllHistory() {
        if (currentHistory.isEmpty()) return
        
        val backupHistory = currentHistory
        
        currentHistory = emptyList()
        adapter.updateList(emptyList())
        layoutNoHistory.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        btnClearAll.visibility = View.GONE

        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        
        backupHistory.forEach { item ->
            val docRef = db.collection("irrigation_history").document(item.id)
            batch.delete(docRef)
        }
        
        batch.commit()
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                fetchHistory()
            }
    }

    private fun showDeleteConfirmation(item: IrrigationItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_history_title)
            .setMessage(R.string.delete_irrigation_record_msg)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteItem(item: IrrigationItem) {
        if (item.id.isEmpty()) return
        
        val updatedList = currentHistory.filter { it.id != item.id }
        currentHistory = updatedList
        adapter.updateList(updatedList)
        
        if (updatedList.isEmpty()) {
            layoutNoHistory.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            btnClearAll.visibility = View.GONE
        }

        FirebaseFirestore.getInstance().collection("irrigation_history")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(requireContext(), "Record deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Sync failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                fetchHistory()
            }
    }

    private fun fetchHistory() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        progressBar.visibility = View.VISIBLE

        // Remove orderBy to avoid index requirement, we sort locally
        historyListener = FirebaseFirestore.getInstance().collection("irrigation_history")
            .whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, e ->
                if (!isAdded) return@addSnapshotListener
                progressBar.visibility = View.GONE
                
                if (e != null) {
                    Log.e("IrrigationFragment", "Error fetching history: ${e.message}")
                    Toast.makeText(requireContext(), "Error loading history: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val historyList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(IrrigationItem::class.java)?.copy(id = doc.id)
                    }.filter { !it.isAI }
                    .sortedByDescending { it.timestamp?.seconds ?: Long.MAX_VALUE } // Sort locally, newest first

                    currentHistory = historyList
                    adapter.updateList(historyList)

                    if (historyList.isEmpty()) {
                        layoutNoHistory.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        btnClearAll.visibility = View.GONE
                    } else {
                        layoutNoHistory.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        btnClearAll.visibility = View.VISIBLE
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historyListener?.remove()
    }
}
