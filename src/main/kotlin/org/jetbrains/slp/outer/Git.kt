package org.jetbrains.slp.outer

import org.jetbrains.slp.train
import org.eclipse.jgit.api.Git as GitAPI
import org.jetbrains.slp.modeling.runners.ModelRunner
import java.io.File

object Git {
    fun trainOnRemoteRepositoriesList(repositoriesList: List<String>, holderDirectory: File, modelRunner: ModelRunner) {
        repositoriesList.forEach {
            trainOnRemoteRepository(it, File(holderDirectory, it.getName()), modelRunner)
        }
    }

    fun trainOnRemoteRepository(url: String, cloneDirectoryPath: File, modelRunner: ModelRunner) {
        GitAPI.cloneRepository()
            .setURI(url)
            .setDirectory(cloneDirectoryPath)
            .call()

        modelRunner.train(cloneDirectoryPath)
    }

    fun clear(holderDirectory: File) {
        holderDirectory.deleteRecursively()
    }

    private fun String.getName(): String {
        return this.split('/').last()
            .split(".git").first()
    }
}
