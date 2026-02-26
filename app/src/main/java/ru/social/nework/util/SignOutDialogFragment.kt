package ru.social.nework.util

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import ru.social.nework.R
import ru.social.nework.auth.AppAuth

class SignOutDialogFragment(val auth: AppAuth) : DialogFragment() {
    @Override
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(getString(R.string.dialog_signOut))
                .setPositiveButton(
                    R.string.sign_out
                ) { _, _ ->
                    auth.removeAuth()
                    findNavController().navigateUp()
                }
                .setNegativeButton(getString(R.string.dialog_cancel)
                ) { _, _ ->
                    // User cancelled the dialog
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}