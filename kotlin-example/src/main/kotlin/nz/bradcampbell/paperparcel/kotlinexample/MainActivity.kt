package nz.bradcampbell.paperparcel.kotlinexample

import android.R
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.TextView
import nz.bradcampbell.paperparcel.PaperParcels
import nz.bradcampbell.paperparcel.TypedParcelable
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    var state = State(0, Date())
    val dateFormat = SimpleDateFormat("HH:mm")


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    savedInstanceState?.let {
      state = PaperParcels.unwrap(it.getParcelable<TypedParcelable<State>>("state"))

      // or:
      // state = PaperParcels.unsafeUnwrap(it.getParcelable<Parcelable>("state"))

      // or:
      // state = it.getParcelable<StateParcel>("state").contents;
    }

    setSupportActionBar(toolbar)

    val plusButton = findViewById(R.id.add_button)
    plusButton.setOnClickListener {
      state = state.copy(state.count + 1, Date())
      updateText()
    }

    val subtractButton = findViewById(R.id.subtract_button)
    subtractButton.setOnClickListener {
      state = state.copy(state.count - 1, Date())
      updateText()
    }

    updateText()
  }

  fun updateText() {
    val counter = findViewById(R.id.counter) as TextView
    counter.text = state.count.toString() + " (updated at " + dateFormat.format(state.modificationDate) + ")"
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    super.onSaveInstanceState(outState)
    outState?.putParcelable("state", PaperParcels.wrap(state))

    // or:
    // outState?.putParcelable("state", StateParcel.wrap(state))
  }
}
