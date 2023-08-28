package net.craftventure.composer.extension

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable


operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}

operator fun CompositeDisposable.minusAssign(disposable: Disposable) {
    remove(disposable)
}
