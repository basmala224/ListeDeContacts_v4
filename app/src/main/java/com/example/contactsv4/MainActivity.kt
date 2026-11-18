package com.example.contactsv4

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.contactsv4.databinding.ActivityMainBinding
import com.example.contactsv4.ui.liste.ListeContactsFragment
import com.example.contactsv4.ui.parametres.ParametresFragment

/**
 * Unique activité : héberge les fragments (FragmentContainerView) et la barre
 * d'application avec l'accès aux Paramètres.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Après une rotation, le FragmentManager restaure la pile : on n'ajoute rien (pas de doublon).
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, ListeContactsFragment(), ListeContactsFragment.TAG)
            }
        }
        supportFragmentManager.addOnBackStackChangedListener {
            majBoutonRetour()
            invalidateOptionsMenu()
        }
        majBoutonRetour()
    }

    fun naviguerVers(fragment: Fragment, tag: String) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            replace(R.id.fragment_container, fragment, tag)
            addToBackStack(tag)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Inutile d'afficher l'icône quand on est déjà sur l'écran Paramètres.
        val surParametres = supportFragmentManager.findFragmentById(R.id.fragment_container) is ParametresFragment
        menu.findItem(R.id.action_parametres)?.isVisible = !surParametres
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_parametres -> {
            naviguerVers(ParametresFragment(), ParametresFragment.TAG)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun majBoutonRetour() {
        supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
