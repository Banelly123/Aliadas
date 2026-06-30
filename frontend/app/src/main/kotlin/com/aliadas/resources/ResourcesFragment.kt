package com.aliadas.resources

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aliadas.R
import com.aliadas.auth.LoginActivity  // Asegúrate de que esta sea la ruta correcta de tu LoginActivity
import com.aliadas.databinding.FragmentResourcesBinding
import com.aliadas.utils.SessionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ResourcesFragment : Fragment() {

    private var _binding: FragmentResourcesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Configuración de Marcadores Telefónicos de Emergencia
        binding.btnCallPolice.setOnClickListener { triggerCall("091") }
        binding.btnCallWoman.setOnClickListener { triggerCall("016") }
        binding.btnCallMedical.setOnClickListener { triggerCall("911") }

        // 2. Enlaces Bento de Artículos Informativos
        binding.cardArticleEmergency.setOnClickListener {
            openWebPage("https://www.aliadas-seguridad.org/emergencias")
        }
        binding.cardArticlePsychology.setOnClickListener {
            openWebPage("https://www.aliadas-seguridad.org/autodefensa")
        }

        // 3. Lógica del Cierre de Sesión (Logout)
        binding.txtLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Limpiar el Token JWT de Railway guardado localmente
            SessionManager.clearSession(requireContext())

            // Limpiar el caché de teléfonos de contactos de confianza
            val prefs = requireContext().getSharedPreferences("aliadas_contacts", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

            // Redirigir de inmediato al LoginActivity borrando el historial de pantallas anteriores
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun triggerCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo abrir el marcador telefónico", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWebPage(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Navegador no disponible para abrir el artículo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}