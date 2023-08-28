package net.craftventure.composer.extension

import io.reactivex.Observable
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList

fun <T> ObservableList<T>.toObservable(): Observable<List<T>> {
    return Observable.create { emitter ->
        emitter.onNext(this)
        val listener = ListChangeListener<T> { it ->
            emitter.onNext(this)
        }
        addListener(listener)
        emitter.setCancellable {
            removeListener(listener)
        }
    }
}

//inline fun <T, R : Comparable<R>> ObservableList<T>.autoSort(crossinline selector: (T) -> R?) {
//    val changeListener = ListChangeListener<T> {
    //        removeListener(changeListener)
//        this.sortBy(selector)
//        addListener(changeListener)
//    }
//    addListener(changeListener)
//
//}