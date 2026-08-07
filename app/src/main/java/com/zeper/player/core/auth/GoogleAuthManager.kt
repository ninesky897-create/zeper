package com.zeper.player.core.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GoogleAuthManager(private val context: Context) {

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    private val _userAccount = MutableStateFlow<GoogleSignInAccount?>(GoogleSignIn.getLastSignedInAccount(context))
    val userAccount: StateFlow<GoogleSignInAccount?> = _userAccount

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>): Result<GoogleSignInAccount> {
        return try {
            val account = completedTask.getResult(ApiException::class.java)
            _userAccount.value = account
            Result.success(account)
        } catch (e: ApiException) {
            Result.failure(e)
        }
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            _userAccount.value = null
            onComplete()
        }
    }
}
