package com.aliadas.contacts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliadas.databinding.FragmentContactsBinding
import com.aliadas.databinding.ItemContactBinding
import com.aliadas.databinding.DialogAddContactBinding
import com.aliadas.network.ContactRequest
import com.aliadas.network.ContactResponse
import com.aliadas.network.RetrofitClient
import com.aliadas.utils.SessionManager
import kotlinx.coroutines.launch

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private val contacts = mutableListOf<ContactResponse>()
    private lateinit var adapter: ContactAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Se inicializa el adaptador pasando la lista y la acción de gestión segura
        adapter = ContactAdapter(contacts) { contact -> 
            showManageContactOptions(contact) 
        }
        
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = adapter

        // Listeners vinculados de forma segura a la vista
        binding.fabAdd.setOnClickListener { showAddContactDialog() }
        binding.swipeRefresh.setOnRefreshListener { loadContacts() }

        loadContacts()
    }

    private fun loadContacts() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            try {
                val token = SessionManager.getBearerToken(requireContext())
                val res = RetrofitClient.api.getContacts(token)
                if (res.isSuccessful) {
                    contacts.clear()
                    contacts.addAll(res.body() ?: emptyList())
                    adapter.notifyDataSetChanged()
                    updateTrustedContactsCache(contacts)
                    binding.tvEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de red con el servidor Railway", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showAddContactDialog() {
        if (contacts.size >= 5) {
            Toast.makeText(requireContext(), "Máximo 5 contactos de confianza", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogAddContactBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar contacto de confianza")
            .setView(dialogBinding.root)
            .setPositiveButton("Agregar") { _, _ ->
                val name = dialogBinding.etName.text.toString().trim()
                val phone = dialogBinding.etPhone.text.toString().trim()
                val relation = dialogBinding.etRelation.text.toString().trim().ifBlank { "Contacto" }
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    addContact(name, phone, relation)
                } else {
                    Toast.makeText(requireContext(), "Nombre y teléfono son requeridos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addContact(name: String, phone: String, relation: String) {
        lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                val res = RetrofitClient.api.addContact(token, ContactRequest(name, phone, relation))
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "✅ Contacto guardado", Toast.LENGTH_SHORT).show()
                    loadContacts()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al guardar el contacto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showManageContactOptions(contact: ContactResponse) {
        val options = arrayOf("Eliminar contacto", "Cancelar")
        AlertDialog.Builder(requireContext())
            .setTitle("Gestionar a ${contact.name}")
            .setItems(options) { dialog, which ->
                if (which == 0) deleteContact(contact)
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteContact(contact: ContactResponse) {
        lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                RetrofitClient.api.deleteContact(token, contact.id)
                loadContacts()
                Toast.makeText(requireContext(), "Contacto eliminado con éxito", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al eliminar el contacto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTrustedContactsCache(contacts: List<ContactResponse>) {
        val prefs = requireContext().getSharedPreferences("aliadas_contacts", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("trusted_phones", contacts.map { it.phone }.toSet()).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ContactAdapter(
    private val items: List<ContactResponse>,
    private val onItemAction: (ContactResponse) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<ContactAdapter.VH>() {

    inner class VH(val binding: ItemContactBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val contact = items[position]
        holder.binding.tvName.text = contact.name
        holder.binding.tvPhone.text = contact.phone
        holder.binding.tvRelation.text = contact.relation
        
        // Control preventivo de clics sobre la tarjeta para evitar desajustes visuales temporales
        holder.root.setOnClickListener { onItemAction(contact) }
    }
}
