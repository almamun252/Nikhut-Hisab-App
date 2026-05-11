package com.almamun252.nikhuthisab.repository

import android.content.Context
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthImpl(
    private val context: Context, // ডাইনামিক স্ট্রিং এর জন্য Context যুক্ত করা হলো
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUser: Flow<AppUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                trySend(
                    AppUser(
                        uid = user.uid,
                        displayName = user.displayName,
                        email = user.email,
                        photoUrl = user.photoUrl?.toString()
                    )
                )
            } else {
                trySend(null)
            }
        }

        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AppUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                Result.success(
                    AppUser(
                        uid = user.uid,
                        displayName = user.displayName,
                        email = user.email,
                        photoUrl = user.photoUrl?.toString()
                    )
                )
            } else {
                // এখানে ডাইনামিক স্ট্রিং ব্যবহার করা হয়েছে
                Result.failure(Exception(context.getString(R.string.msg_user_not_found)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}