package com.ajsherrell.android.adventcode

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

// https://medium.com/@gabrieldemattosleon/fundamentals-of-rxjava-with-kotlin-for-absolute-beginners-3d811350b701

class LearnRx : AppCompatActivity() {

    val source = PublishSubject.create<String>()//when I use these, nothing shows on screen.
    val disposables = CompositeDisposable()

    private lateinit var learnRxTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn_rx)
        learnRxTv = findViewById(R.id.learnRx)

        //Try different methods here:
        learnRxTv.text = repeatIt()
    }

    private fun getObservableFromList(myList: List<String>) =
        Observable.create<String> { emitter ->
            myList.forEach { kind ->
                if (kind == "") {
                    emitter.onError(Exception("No value to show."))
                }
                emitter.onNext(kind)
            }
            emitter.onComplete()
        }

    private fun observableSB(): String {
        var sb = StringBuilder()
        Observable.just("Shopping List:")
            .subscribeOn(Schedulers.computation())//the # of threads for reuse are fixed to the # of cores in device.
            .subscribe { value -> sb.append("$value\n\n") }

        Observable.just("Apple", "Orange", "Banana")//not working!
            //.map({ input -> throw RuntimeException() } ) //creates error.
            .subscribeOn(Schedulers.io())//IO uses a thread pool where threads are reused.
            .observeOn(AndroidSchedulers.mainThread())//switches operation back to UI.
            .subscribe(
                { value -> sb.append("$value\n") }, // onNext
                { error -> sb.append("$error\n") },    // onError
                { sb.append("Completed!\n") }                 // onComplete
            )

        Observable.fromArray("Pear\n", "Cherry\n", "Blossom\n")
            .subscribe { sb.append(it) }

        Observable.fromIterable(listOf("Eggs", "Rose", "Sake"))
            .subscribeOn(Schedulers.single())//All will run on a single thread.
            .subscribe(
                { value -> sb.append("$value\n") }, // onNext
                { error -> sb.append("$error\n") },    // onError
                { sb.append("Completed!\n") }                 // onComplete
            )

        getObservableFromList(listOf("Milk", "Tea", "Coffee"))
            .subscribe { sb.append("$it\n") }

        getObservableFromList(listOf("Salt", "Salsa", "Pepper")) //use empty string to create error.
            .subscribeOn(Schedulers.trampoline())//used to run "in order" on current thread.
            .subscribe(
                { v -> sb.append("$v\n") },
                { e -> sb.append("$e\n") }
            )

        Observable.intervalRange(10L, 5L, 0L, 1L,
            TimeUnit.MICROSECONDS).subscribe { sb.append("$it\n") } //not showing up.
        return sb.toString()
    }

    private fun outOfMemory(): String {
        var sb = StringBuilder()
        val observable = PublishSubject.create<Int>()
        observable
            .toFlowable(BackpressureStrategy.DROP)//drops some items to preserve memory.
            .observeOn(Schedulers.computation())
            .subscribe(
                { sb.append("The number is: $it\n") },
                {t->
                    sb.append("Throwable: ${t.message}\n")
                }
            )
        for (i in 0..1000000) {
            observable.onNext(i)
        }
        return sb.toString()
    }

    private fun flowIt(): String {//supports backpressure
        var sb = StringBuilder()
        Flowable.just("This is a Flowable!")
            .subscribe(
                { value -> sb.append("Received: $value\n") },
                { error -> sb.append("Error: $error\n") },
                { sb.append("Completed!") }
            )

        return sb.toString()
    }

    private fun maybeMaybe(): String { //Maybe returns a single optional value.
        var sb = StringBuilder()
        Maybe.just("This is a maybe...")
            .subscribe(
                { value -> sb.append("Received: $value\n") },
                { error -> sb.append("Error: $error\n") },
                { sb.append("Completed.") }
            )

        return sb.toString()
    }

    private fun singleAndLooking(): String {//Single is used to return a single value.
        var sb = StringBuilder()
        Single.just("This is a single.")
            .subscribe(
                { v -> sb.append("Value is: $v\n") },
                { e -> sb.append("Error: $e") }
            )

        return sb.toString()
    }

    private fun moreThanMeetsTheEye(): String {//a transformer repeats commonly used chains. --not working!
        var sb = StringBuilder()
        Observable.just("Apple", "Orange", "Banana")
            .compose(applyObservableAsync())
            .subscribe { v -> sb.append("The first observable received: $v\n") }

        Observable.just("Water", "Fire", "Wood")
            .compose(applyObservableAsync())
            .subscribe { v -> sb.append("The second observable received: $v\n") }

        return sb.toString()
    }

    private fun <T> applyObservableAsync(): ObservableTransformer<T, T> {
        return ObservableTransformer { observable ->
            observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        }
    }

    private fun mapper(): String {//map transforms values emitted by a stream into a single value. --not working!
        var sb = StringBuilder()
        Observable.just("Air", "Earth", "Heart")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { m -> m + " 2" }
            .subscribe { v -> sb.append("Received: $v\n") } //or use a flatmap to transform generic into observable.
//            .flatMap { m ->
//                Observable.just(m + " 2")
//                    .subscribeOn(Schedulers.io())
        return sb.toString()
    }

    private fun zipItGood(): String {//"zips" the values together.
        var sb = StringBuilder()
        Observable.zip(
            Observable.just(
                "Roses", "Sunflowers", "Leaves", "Clouds", "Violets",
                "Plastics"),
            Observable.just(
                "Red", "Yellow", "Green", "White or Grey", "Purple"),
            BiFunction<String, String, String> { type, color ->
                "$type are $color"
            })
            .subscribe { v -> sb.append("Received: $v\n") }

        return sb.toString()
    }

    private fun concatObs(): String {
        var sb = StringBuilder()
        val test1 = Observable.just("Apple", "Orange", "Banana")
        val test2 = Observable.just("Microsoft", "Google")
        val test3 = Observable.just("Grass", "Tree", "Flower", "Sunflower")

        Observable.concat(test1, test2, test3)
            .subscribe{ x -> sb.append("\nReceived " + x) }

        return sb.toString()
    }

    private fun mergeIt(): String { //not working!
        var sb = StringBuilder()
        Observable.merge(
            Observable.interval(250, TimeUnit.MILLISECONDS).map { i -> "Apple" },
            Observable.interval(150, TimeUnit.MILLISECONDS).map { i -> "Orange" })
            .take(10)
            .subscribe{ v -> sb.append("Received: $v\n") }

        return sb.toString()
    }

    private fun filterIt(): String {
        var sb = StringBuilder()
        Observable.just(2, 30, 22, 5, 60, 1)
            .filter{ x -> x < 10 }
            .subscribe{ x -> sb.append("\nReceived: " + x) }

        return sb.toString()
    }

    private fun repeatIt(): String {
        var sb = StringBuilder()
        Observable.just("Apple", "Orange", "Banana")
            .repeat(2)
            .subscribe { v -> sb.append("\nReceived: $v") }

        return sb.toString()
    }

    private fun takeIt(): String {
        var sb = StringBuilder()
        Observable.just("Apple", "Orange", "Banana")
            .take(2)
            .subscribe { v -> sb.append("\nReceived: $v") }

        return sb.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}