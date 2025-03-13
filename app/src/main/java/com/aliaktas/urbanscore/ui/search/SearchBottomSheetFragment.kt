package com.aliaktas.urbanscore.ui.search

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CityModel
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SearchBottomSheetFragment : BottomSheetDialogFragment() {

    // UI Components
    private lateinit var editTextSearch: EditText
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var progressBarSearch: ProgressBar
    private lateinit var btnClearSearch: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var contentContainer: ConstraintLayout

    // Data and Adapters
    private val searchAdapter = SearchResultsAdapter()
    private var allCities = mutableListOf<CityModel>()
    private var onCitySelectedListener: ((String) -> Unit)? = null

    companion object {
        fun newInstance(cities: List<CityModel>, onCitySelected: (String) -> Unit): SearchBottomSheetFragment {
            return SearchBottomSheetFragment().apply {
                this.allCities.clear()
                this.allCities.addAll(cities)
                this.onCitySelectedListener = onCitySelected
            }
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // Set full screen with transparent background
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog

            // Setup bottom sheet behavior
            setupFullScreenBottomSheet(bottomSheetDialog)
        }

        return dialog
    }

    private fun setupFullScreenBottomSheet(bottomSheetDialog: BottomSheetDialog) {
        // Get the BottomSheet view
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        // Remove background to make it transparent
        bottomSheet.background = null

        // Get BottomSheetBehavior
        val behavior = BottomSheetBehavior.from(bottomSheet)

        // Setup the layout params to match screen height
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams

        // Configure behavior
        behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isHideable = true

            // Hide when dragged down too much
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dismiss()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Optionally adjust UI based on slide position
                    if (slideOffset < 0.9f) {
                        hideKeyboard()
                    }
                }
            })
        }

        // Set dialog window properties
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.DialogAnimation

            // Set soft input mode to adjust resize
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        editTextSearch = view.findViewById(R.id.editTextSearch)
        recyclerViewResults = view.findViewById(R.id.recyclerViewResults)
        tvNoResults = view.findViewById(R.id.tvNoResults)
        progressBarSearch = view.findViewById(R.id.progressBarSearch)
        btnClearSearch = view.findViewById(R.id.btnClearSearch)
        btnBack = view.findViewById(R.id.btnBack)
        contentContainer = view.findViewById(R.id.contentContainer)

        // Setup UI
        setupRecyclerView()
        setupListeners()

        // Show keyboard after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            showKeyboard()
        }, 300)
    }

    private fun setupRecyclerView() {
        // Setup RecyclerView and adapter
        recyclerViewResults.adapter = searchAdapter

        // Setup item click listener
        searchAdapter.onItemClick = { city ->
            onCitySelectedListener?.invoke(city.id)
            dismiss()
        }
    }

    private fun setupListeners() {
        // Back button (close sheet)
        btnBack.setOnClickListener {
            hideKeyboard()
            dismiss()
        }

        // Clear search button
        btnClearSearch.setOnClickListener {
            editTextSearch.text.clear()
            editTextSearch.requestFocus()
            showKeyboard()
        }

        // Search text changed listener
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase(Locale.getDefault())

                // Show/hide clear button
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                // Perform search if 2+ characters
                if (query.length >= 2) {
                    performSearch(query)
                } else {
                    searchAdapter.submitList(emptyList())
                    tvNoResults.visibility = View.GONE
                }
            }
        })

        // Search action on keyboard
        editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = editTextSearch.text.toString().trim()
                if (query.length >= 2) {
                    performSearch(query)
                    hideKeyboard()
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun performSearch(query: String) {
        progressBarSearch.visibility = View.VISIBLE
        recyclerViewResults.visibility = View.GONE
        tvNoResults.visibility = View.GONE

        // Use coroutines for background filtering
        viewLifecycleOwner.lifecycleScope.launch {
            // Do filtering in background thread
            val results = withContext(Dispatchers.Default) {
                allCities.filter { city ->
                    city.cityName.lowercase(Locale.getDefault()).contains(query) ||
                            city.country.lowercase(Locale.getDefault()).contains(query)
                }
            }

            // Update UI in main thread
            progressBarSearch.visibility = View.GONE
            recyclerViewResults.visibility = View.VISIBLE
            tvNoResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE

            searchAdapter.submitList(results)
        }
    }

    private fun showKeyboard() {
        editTextSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTextSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    // Clean up resources when fragment is destroyed
    override fun onDestroyView() {
        hideKeyboard()
        recyclerViewResults.adapter = null
        super.onDestroyView()
    }

    // Adapter for search results
    private inner class SearchResultsAdapter :
        ListAdapter<CityModel, SearchResultsAdapter.ViewHolder>(
            object : DiffUtil.ItemCallback<CityModel>() {
                override fun areItemsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(oldItem: CityModel, newItem: CityModel): Boolean {
                    return oldItem == newItem
                }
            }
        ) {

        var onItemClick: ((CityModel) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
            private val tvCountry: TextView = itemView.findViewById(R.id.tvCountry)
            private val tvRating: TextView = itemView.findViewById(R.id.tvRating)
            private val ivFlag: ImageView = itemView.findViewById(R.id.ivFlag)

            init {
                itemView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick?.invoke(getItem(position))
                    }
                }
            }

            fun bind(city: CityModel) {
                tvCityName.text = city.cityName
                tvCountry.text = city.country
                tvRating.text = String.format("%.1f", city.averageRating)

                // Load flag image
                Glide.with(itemView)
                    .load(city.flagUrl)
                    .into(ivFlag)
            }
        }
    }
}