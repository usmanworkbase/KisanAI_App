package com.example.kisanai

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutNoHistory: View
    private lateinit var btnClearAll: View
    private var currentHistory: List<HistoryItem> = emptyList()
    private var diseases: List<Disease> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        
        recyclerView = view.findViewById(R.id.rvHistory)
        progressBar = view.findViewById(R.id.pbHistory)
        layoutNoHistory = view.findViewById(R.id.layoutNoHistory)
        btnClearAll = view.findViewById(R.id.btnClearAll)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter(emptyList(), { item ->
            onItemClicked(item)
        }, { item ->
            showDeleteConfirmation(item)
        })
        recyclerView.adapter = adapter

        btnClearAll.setOnClickListener {
            showClearAllConfirmation()
        }

        loadDiseases()
        fetchHistory()

        return view
    }

    private fun loadDiseases() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonString = requireContext().assets.open("disease.json").bufferedReader().use { it.readText() }
                val response = Gson().fromJson(jsonString, DiseaseResponse::class.java)
                diseases = response.diseases ?: emptyList()
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        adapter.setDiseases(diseases)
                    }
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Error loading diseases: ${e.message}")
            }
        }
    }

    private fun onItemClicked(item: HistoryItem) {
        val foundDisease = diseases.find { 
            it.class_name == item.rawLabel || it.display_name == item.disease 
        }
        
        if (foundDisease != null && foundDisease.severity != "None") {
            val intent = Intent(requireContext(), DiseaseDetailActivity::class.java)
            intent.putExtra("disease_data", foundDisease)
            intent.putExtra("image_uri", item.imageUrl)
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "No details available for this item", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_history_title)
            .setMessage(R.string.clear_history_msg)
            .setPositiveButton(R.string.clear_all) { _, _ ->
                clearAllHistory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun clearAllHistory() {
        if (currentHistory.isEmpty()) return
        
        // Keep a copy in case deletion fails
        val backupHistory = currentHistory
        
        // 1. Immediately clear UI
        currentHistory = emptyList()
        adapter.updateList(emptyList())
        layoutNoHistory.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        btnClearAll.visibility = View.GONE

        // 2. Delete from Cloud
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        
        backupHistory.forEach { item ->
            val docRef = db.collection("history").document(item.id)
            batch.delete(docRef)
        }
        
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                fetchHistory() // Refresh to restore items if cloud delete failed
            }
    }

    private fun showDeleteConfirmation(item: HistoryItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_history_title)
            .setMessage(R.string.delete_history_msg)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteItem(item: HistoryItem) {
        if (item.id.isEmpty()) return
        
        // 1. Immediately update the UI for a fast feel
        val updatedList = currentHistory.filter { it.id != item.id }
        currentHistory = updatedList
        adapter.updateList(updatedList)
        
        if (updatedList.isEmpty()) {
            layoutNoHistory.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            btnClearAll.visibility = View.GONE
        }

        // 2. Perform the actual delete in Firestore
        FirebaseFirestore.getInstance().collection("history")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Sync failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                // If there's an error, refresh from cloud to restore the item
                fetchHistory()
            }
    }

    private fun fetchHistory() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        progressBar.visibility = View.VISIBLE

        FirebaseFirestore.getInstance().collection("history")
            .whereEqualTo("userId", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (!isAdded) return@addSnapshotListener
                progressBar.visibility = View.GONE
                if (e != null) {
                    Log.e("HistoryFragment", "Error fetching history", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val historyList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(HistoryItem::class.java)?.copy(id = doc.id)
                    }

                    currentHistory = historyList

                    if (historyList.isEmpty()) {
                        layoutNoHistory.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        btnClearAll.visibility = View.GONE
                    } else {
                        layoutNoHistory.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        btnClearAll.visibility = View.VISIBLE
                        adapter.updateList(historyList)
                    }
                }
            }
    }
}
