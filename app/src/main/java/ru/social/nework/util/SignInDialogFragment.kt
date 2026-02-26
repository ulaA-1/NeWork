package ru.social.nework.util

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import ru.social.nework.R

class SignInDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(getString(R.string.sign_in_dialog))
                .setNegativeButton(R.string.dialog_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.sign_up) { _, _ ->
                    findNavController().navigate(R.id.action_global_signUpFragment)
                }
                .setPositiveButton(R.string.sign_in) { _, _ ->
                    findNavController().navigate(R.id.action_global_signInFragment)
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}