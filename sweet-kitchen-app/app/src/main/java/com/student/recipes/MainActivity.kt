package com.student.recipes

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray

// One dessert from assets/recipes.json
data class Recipe(
    val name: String,
    val category: String,
    val emoji: String,
    val ingredients: List<String>,
    val steps: List<String>
)

class MainActivity : Activity() {

    private val categories = listOf(
        "All" to "🍰 All Desserts",
        "Turkish Desserts" to "🇹🇷 Turkish Desserts",
        "Cakes" to "🍰 Cakes",
        "Cookies" to "🍪 Cookies",
        "Milk Desserts" to "🥛 Milk Desserts",
        "Chocolate Desserts" to "🍫 Chocolate",
        "International Desserts" to "🌎 International"
    )

    private lateinit var recipes: List<Recipe>
    private lateinit var input: EditText
    private lateinit var onlyComplete: CheckBox
    private lateinit var resultsTitle: TextView
    private lateinit var recipeCount: TextView
    private lateinit var results: LinearLayout
    private lateinit var categoryRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.ingredientInput)
        onlyComplete = findViewById(R.id.onlyComplete)
        resultsTitle = findViewById(R.id.resultsTitle)
        recipeCount = findViewById(R.id.recipeCount)
        results = findViewById(R.id.results)
        categoryRow = findViewById(R.id.categoryRow)

        findViewById<View>(R.id.searchBox).background = rounded(getColor(R.color.card), 20f)

        recipes = loadRecipes()

        findViewById<Button>(R.id.findButton).setOnClickListener { findDesserts() }
        findViewById<Button>(R.id.clearButton).setOnClickListener { clearSearch() }

        buildCategoryButtons()
        filterCategory("All")
    }

    // Read the dessert list from assets/recipes.json
    private fun loadRecipes(): List<Recipe> {
        val text = assets.open("recipes.json").bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Recipe(
                name = o.getString("name"),
                category = o.getString("category"),
                emoji = o.optString("emoji", "🍰"),
                ingredients = o.getJSONArray("ingredients").toStringList(),
                steps = o.getJSONArray("steps").toStringList()
            )
        }
    }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }

    private fun normalize(text: String) =
        text.lowercase().trim().replace(Regex("\\s+"), " ")

    // Same matching rule as the website
    private fun hasIngredient(ingredient: String, userIngredients: List<String>) =
        userIngredients.any { user ->
            normalize(ingredient).contains(user) || user.contains(normalize(ingredient))
        }

    private fun findDesserts() {
        hideKeyboard()
        val userIngredients = input.text.toString()
            .split(",")
            .map { normalize(it) }
            .filter { it.isNotEmpty() }

        val complete = onlyComplete.isChecked

        val found = recipes
            .map { r -> r to r.ingredients.count { hasIngredient(it, userIngredients) } }
            .filter { (r, count) -> if (complete) count == r.ingredients.size else count > 0 }
            .sortedByDescending { (r, count) -> count * 100 / r.ingredients.size }
            .map { it.first }

        resultsTitle.text =
            if (complete) "🍰 Desserts You Can Make"
            else "🍰 Desserts Based on Your Ingredients"

        selectCategoryButton(null)
        showRecipes(found, userIngredients)
    }

    private fun clearSearch() {
        input.setText("")
        onlyComplete.isChecked = false
        filterCategory("All")
    }

    private fun filterCategory(category: String) {
        val list = if (category == "All") recipes else recipes.filter { it.category == category }
        resultsTitle.text = if (category == "All") "🍰 Dessert Menu" else "🍰 $category"
        selectCategoryButton(category)
        showRecipes(list, emptyList())
    }

    private fun buildCategoryButtons() {
        for ((key, label) in categories) {
            val b = TextView(this).apply {
                text = label
                tag = key
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(16), dp(10), dp(16), dp(10))
                setOnClickListener { filterCategory(key) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(dp(4), 0, dp(4), 0)
            categoryRow.addView(b, lp)
        }
    }

    private fun selectCategoryButton(selected: String?) {
        for (i in 0 until categoryRow.childCount) {
            val b = categoryRow.getChildAt(i) as TextView
            val active = b.tag == selected
            b.background = rounded(getColor(if (active) R.color.primary else R.color.card), 12f)
            b.setTextColor(getColor(if (active) R.color.white else R.color.title))
        }
    }

    private fun showRecipes(list: List<Recipe>, userIngredients: List<String>) {
        results.removeAllViews()
        recipeCount.text = "${list.size} desserts"

        if (list.isEmpty()) {
            results.addView(text("😕\nNo matching desserts found.\nTry adding more ingredients to your kitchen list.", 16f, R.color.muted).apply {
                gravity = android.view.Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
                background = rounded(getColor(R.color.card), 18f)
            })
            return
        }

        for (r in list) results.addView(recipeCard(r, userIngredients))
    }

    private fun recipeCard(r: Recipe, userIngredients: List<String>): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(getColor(R.color.card), 18f)
            setPadding(dp(18), dp(14), dp(18), dp(18))
            elevation = dp(3).toFloat()
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, 0, dp(16))
        card.layoutParams = lp

        card.addView(text(r.emoji, 48f, R.color.text).apply {
            gravity = android.view.Gravity.CENTER
            background = rounded(getColor(R.color.chip), 14f)
            setPadding(0, dp(12), 0, dp(12))
        })
        card.addView(text(r.name, 21f, R.color.title, bold = true).apply { setPadding(0, dp(10), 0, dp(4)) })
        card.addView(pill(r.category, R.color.chip, R.color.title))

        // Steps are hidden until the card is tapped
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        if (userIngredients.isNotEmpty()) {
            val matched = r.ingredients.filter { hasIngredient(it, userIngredients) }
            val missing = r.ingredients.filterNot { hasIngredient(it, userIngredients) }
            card.addView(pill("✅ You have ${matched.size} / ${r.ingredients.size} ingredients", R.color.match_bg, R.color.match_text))
            if (missing.isNotEmpty()) {
                card.addView(pill("🛒 Missing: ${missing.joinToString(", ")}", R.color.missing_bg, R.color.missing_text))
            }
        }

        card.addView(text("Ingredients", 16f, R.color.text, bold = true).apply { setPadding(0, dp(10), 0, dp(2)) })
        card.addView(text(r.ingredients.joinToString("\n") { "• $it" }, 15f, R.color.muted))

        details.addView(text("Steps", 16f, R.color.text, bold = true).apply { setPadding(0, dp(10), 0, dp(2)) })
        details.addView(text(r.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n"), 15f, R.color.muted))
        card.addView(details)

        val toggle = text("▼ Show steps", 14f, R.color.primary, bold = true).apply { setPadding(0, dp(10), 0, 0) }
        card.addView(toggle)
        card.setOnClickListener {
            val open = details.visibility == View.VISIBLE
            details.visibility = if (open) View.GONE else View.VISIBLE
            toggle.text = if (open) "▼ Show steps" else "▲ Hide steps"
        }
        return card
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(getColor(color))
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun pill(value: String, bg: Int, fg: Int): View {
        val t = text(value, 13f, fg).apply {
            background = rounded(getColor(bg), 20f)
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, dp(4), 0, dp(4))
        t.layoutParams = lp
        return t
    }

    private fun rounded(color: Int, radiusDp: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusDp * resources.displayMetrics.density
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
    }
}
