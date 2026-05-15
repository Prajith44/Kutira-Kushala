package com.example.kutira_kushala

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProductsFragment : Fragment() {

    private lateinit var adapter: ProductAdapter
    private lateinit var suggestionAdapter: SuggestionAdapter
    private var currentCategory = "All"
    private var currentQuery = ""
    
    private lateinit var etSearch: EditText
    private lateinit var llNoResults: LinearLayout
    private lateinit var cvSuggestions: CardView
    private lateinit var rvSuggestions: RecyclerView
    
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var mainContent: View
    
    private val recentSearches = mutableListOf<String>()
    private var allProductsFromDb = listOf<ProductItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_products, container, false)
        
        shimmerContainer = view.findViewById(R.id.shimmer_view_container)
        mainContent = view.findViewById(R.id.main_content)

        etSearch = view.findViewById(R.id.et_search)
        llNoResults = view.findViewById(R.id.ll_no_results)
        cvSuggestions = view.findViewById(R.id.cv_search_suggestions)
        rvSuggestions = view.findViewById(R.id.rv_suggestions)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_product_grid)
        adapter = ProductAdapter { product ->
            addToRecentSearches(product.name)
            val intent = Intent(context, ProductDetailActivity::class.java)
            if (product.productId.isNotEmpty()) {
                intent.putExtra("PRODUCT_ID", product.productId)
            } else {
                intent.putExtra("PRODUCT_NAME", product.name)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = adapter
        
        setupSearch()
        setupFilters(view)
        setupSuggestions()
        
        loadProductsFromFirestore()
        simulateLoading()
        
        return view
    }

    private fun loadProductsFromFirestore() {
        FirebaseFirestore.getInstance().collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                allProductsFromDb = snapshot.toObjects(ProductItem::class.java)
                applyFilters()
            }
    }

    private fun simulateLoading() {
        shimmerContainer.startShimmer()
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                shimmerContainer.stopShimmer()
                shimmerContainer.visibility = View.GONE
                mainContent.visibility = View.VISIBLE
                applyFilters()
            }
        }, 1200)
    }

    override fun onResume() {
        super.onResume()
        if (mainContent.visibility == View.VISIBLE) {
            applyFilters()
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.lowercase() ?: ""
                applyFilters()
                updateSuggestions(currentQuery)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && currentQuery.isEmpty() && recentSearches.isNotEmpty()) {
                showSuggestions(recentSearches)
            } else if (!hasFocus) {
                cvSuggestions.visibility = View.GONE
            }
        }
    }

    private fun setupSuggestions() {
        suggestionAdapter = SuggestionAdapter(emptyList()) { selected ->
            etSearch.setText(selected)
            etSearch.setSelection(selected.length)
            cvSuggestions.visibility = View.GONE
            currentQuery = selected.lowercase()
            applyFilters()
        }
        rvSuggestions.layoutManager = LinearLayoutManager(context)
        rvSuggestions.adapter = suggestionAdapter
    }

    private fun updateSuggestions(query: String) {
        if (query.isEmpty()) {
            if (recentSearches.isNotEmpty()) {
                showSuggestions(recentSearches)
            } else {
                cvSuggestions.visibility = View.GONE
            }
            return
        }

        val matches = allProductsFromDb
            .map { it.name }
            .filter { it.lowercase().contains(query) }
            .distinct()
            .take(5)

        if (matches.isNotEmpty()) {
            showSuggestions(matches)
        } else {
            cvSuggestions.visibility = View.GONE
        }
    }

    private fun showSuggestions(list: List<String>) {
        suggestionAdapter.updateData(list)
        cvSuggestions.visibility = View.VISIBLE
    }

    private fun addToRecentSearches(query: String) {
        if (query.isBlank()) return
        recentSearches.remove(query)
        recentSearches.add(0, query)
        if (recentSearches.size > 5) recentSearches.removeAt(5)
    }

    private fun setupFilters(view: View) {
        val filterAll = view.findViewById<TextView>(R.id.filter_all)
        val filterCrafts = view.findViewById<TextView>(R.id.filter_crafts)
        val filterFood = view.findViewById<TextView>(R.id.filter_food)
        val filterTextiles = view.findViewById<TextView>(R.id.filter_textiles)
        
        val filters = listOf(filterAll, filterCrafts, filterFood, filterTextiles)
        
        filters.forEach { filter ->
            filter.setOnClickListener {
                val tv = it as TextView
                currentCategory = tv.text.toString()
                updateFilterUI(tv, filters)
                applyFilters()
            }
        }
    }

    private fun updateFilterUI(selected: TextView, allFilters: List<TextView>) {
        val isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        allFilters.forEach {
            it.setBackgroundResource(R.drawable.filter_chip_unselected)
            it.setTextColor(if (isDark) Color.parseColor("#9CA3AF") else Color.parseColor("#6B7280"))
        }
        selected.setBackgroundResource(R.drawable.filter_chip_selected)
        selected.setTextColor(Color.WHITE)
    }

    private fun applyFilters() {
        val filtered = allProductsFromDb.filter { product ->
            val matchesCategory = currentCategory == "All" || product.category == currentCategory
            val matchesSearch = currentQuery.isEmpty() || 
                               product.name.lowercase().contains(currentQuery) || 
                               product.category.lowercase().contains(currentQuery)
            
            matchesCategory && matchesSearch
        }
        
        adapter.submitList(filtered)
        llNoResults.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
