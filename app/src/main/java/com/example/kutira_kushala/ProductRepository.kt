package com.example.kutira_kushala

object ProductRepository {
    private val demoImages = listOf(
        R.drawable.ic_shield,
        android.R.drawable.ic_menu_camera,
        android.R.drawable.ic_menu_gallery,
        android.R.drawable.ic_menu_slideshow
    )

    private val products = mutableListOf<ProductItem>(
        ProductItem("Bamboo Basket", "Crafts", "₹450", additionalImages = demoImages),
        ProductItem("Clay Pot", "Crafts", "₹300", additionalImages = demoImages),
        ProductItem("Wooden Toy", "Crafts", "₹800", additionalImages = demoImages),
        ProductItem("Handmade Lamp", "Crafts", "₹1,200", additionalImages = demoImages),
        ProductItem("Coconut Shell Craft", "Crafts", "₹650", additionalImages = demoImages),
        ProductItem("Homemade Pickles", "Food", "₹180", additionalImages = demoImages),
        ProductItem("Papad", "Food", "₹120", additionalImages = demoImages),
        ProductItem("Millet Snacks", "Food", "₹250", additionalImages = demoImages),
        ProductItem("Traditional Sweets", "Food", "₹400", additionalImages = demoImages),
        ProductItem("Organic Honey", "Food", "₹550", additionalImages = demoImages),
        ProductItem("Silk Saree", "Textiles", "₹5,000", additionalImages = demoImages),
        ProductItem("Handwoven Shawl", "Textiles", "₹2,500", additionalImages = demoImages),
        ProductItem("Cotton Bags", "Textiles", "₹350", additionalImages = demoImages),
        ProductItem("Embroidered Fabric", "Textiles", "₹1,800", additionalImages = demoImages),
        ProductItem("Traditional Scarf", "Textiles", "₹600", additionalImages = demoImages)
    )

    fun getAllProducts(): List<ProductItem> = products.toList()

    fun addProduct(product: ProductItem) {
        products.add(0, product)
    }
}
