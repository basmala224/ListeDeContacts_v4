package com.example.contactsv4.ui.liste

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contactsv4.R
import com.example.contactsv4.databinding.ItemContactBinding
import com.example.contactsv4.model.Contact
import com.example.contactsv4.util.Affichage
import com.example.contactsv4.util.ImageUtils

/**
 * Adapter de la RecyclerView. Il reçoit des préférences DÉJÀ converties ([Affichage]) :
 * taille en sp, couleur en Int, booléen pour le bouton Appeler.
 * Les clics transmettent le Contact (donc son _id), jamais la position.
 */
class ContactAdapter(
    private val onClick: (Contact) -> Unit,
    private val onAppeler: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(DIFF) {

    var affichage: Affichage? = null
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount) // réapplique les réglages aux lignes visibles
            }
        }

    inner class ContactViewHolder(private val b: ItemContactBinding) : RecyclerView.ViewHolder(b.root) {
        private var courant: Contact? = null

        init {
            b.root.setOnClickListener { courant?.let(onClick) }
            // Le bouton consomme son clic : il n'ouvre pas la fiche détaillée.
            b.btnAppelerLigne.setOnClickListener { courant?.let(onAppeler) }
        }

        fun bind(c: Contact) {
            courant = c
            b.tvNom.text = c.nom
            b.tvPrenom.text = c.prenom
            b.tvTelephone.text = c.telephone
            b.ivFavori.isVisible = c.favori
            b.ivGps.alpha = if (c.aUnePosition) 1f else 0.2f
            b.ivGps.contentDescription = b.root.context.getString(
                if (c.aUnePosition) R.string.cd_position_ok else R.string.cd_position_absente
            )
            ImageUtils.afficher(b.ivPhoto, c.photoUri, 160)

            affichage?.let { a ->
                listOf(b.tvNom, b.tvPrenom, b.tvTelephone).forEach {
                    it.setTextSize(TypedValue.COMPLEX_UNIT_SP, a.tailleSp)
                }
                b.tvNom.setTextColor(a.couleur)
                b.tvPrenom.setTextColor(a.couleur)
                b.btnAppelerLigne.isVisible = a.appelDepuisListe   // GONE si désactivé
                b.btnAppelerLigne.contentDescription =
                    b.root.context.getString(R.string.cd_appeler, c.nomComplet)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ContactViewHolder(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(a: Contact, b: Contact) = a.id == b.id
            override fun areContentsTheSame(a: Contact, b: Contact) = a == b
        }
    }
}
