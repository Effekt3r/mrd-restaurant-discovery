package com.effekt3r.mrd.tools.datagen

/** A dish a course can list, with the description its item carries when it has one. */
internal class Dish(val name: String, val description: String)

/** Where a course's photos come from. The pool has only meal photos, so drinks have none. */
internal enum class Photos { RESTAURANT, DESSERT, NONE }

/**
 * A menu category as a concept serves it: the dishes it can list, in menu order; its prices, in whole rands; where its
 * photos come from; and the options its items can offer.
 */
internal class Course(
    val name: String,
    val rands: IntRange,
    val photos: Photos,
    val options: List<String>,
    val dishes: List<Dish>
)

/** Each concept's courses, in menu order. Dish names are generic and never a brand's. */
internal object Courses {

    fun of(concept: Concept): List<Course> = byConcept.getValue(concept.key)

    val byConcept: Map<String, List<Course>> = mapOf(
        "pizza" to listOf(PIZZAS, SIDES, DESSERTS, DRINKS),
        "burgers" to listOf(BURGERS, SIDES, MILKSHAKES, DRINKS),
        "chicken" to listOf(FLAME_GRILLED, SIDES, DRINKS),
        "sushi" to listOf(PLATTERS, ROLLS, SASHIMI, DRINKS),
        "indian" to listOf(INDIAN_STARTERS, INDIAN_CURRIES, BREADS_AND_RICE, DRINKS),
        "chinese" to listOf(DIM_SUM, NOODLES, WOK, DRINKS),
        "italian" to listOf(ANTIPASTI, PASTA, DOLCI, DRINKS),
        "steakhouse" to listOf(GRILL_STARTERS, STEAKS, BURGERS, SIDES, DESSERTS),
        "seafood" to listOf(SEAFOOD_STARTERS, FISH_AND_CHIPS, SEAFOOD_GRILLS, SIDES, DRINKS),
        "cafe" to listOf(BREAKFAST, LIGHT_MEALS, BAKERY, COFFEE_AND_TEA),
        "healthy" to listOf(BOWLS, SALADS, SMOOTHIES),
        "mexican" to listOf(TACOS, BURRITOS, SIDES, DRINKS),
        "thai" to listOf(THAI_STARTERS, THAI_CURRIES, THAI_NOODLES, DRINKS),
        "middle-eastern" to listOf(MEZZE, SHAWARMA, GRILLS, SWEETS),
        "cape-malay" to listOf(CAPE_MALAY_CURRIES, BREYANI, GATSBYS, SIDES, DESSERTS),
        "desserts" to listOf(CAKES, WAFFLES, ICE_CREAM, COFFEE_AND_TEA)
    )
}

private fun course(
    name: String,
    rands: IntRange,
    photos: Photos,
    options: List<String>,
    vararg dishes: Pair<String, String>
) = Course(name, rands, photos, options, dishes.map { (dish, description) -> Dish(dish, description) })

private val DRINKS = course(
    "Drinks", 20..45, Photos.NONE, listOf("Large", "No ice", "Extra lemon", "Add mint"),
    "Still Water" to "500 ml, chilled",
    "Sparkling Water" to "500 ml, with a slice of lemon",
    "Rooibos Iced Tea" to "Brewed in-house with honey and lemon",
    "Ginger Beer" to "Home-brewed with fresh ginger",
    "Homemade Lemonade" to "Fresh lemons, cane sugar and soda",
    "Rock Shandy" to "Lemonade, soda water and bitters",
    "Fresh Orange Juice" to "Squeezed to order",
    "Cream Soda Float" to "Green cream soda with vanilla ice cream"
)

private val DESSERTS = course(
    "Desserts", 45..95, Photos.DESSERT, listOf("Add vanilla ice cream", "Add whipped cream", "Extra custard"),
    "Malva Pudding" to "Warm apricot sponge with custard",
    "Milk Tart" to "Cinnamon-dusted custard tart",
    "Koeksisters" to "Plaited and syrup-soaked, two per portion",
    "Cape Brandy Pudding" to "Dates, pecans and a brandy syrup",
    "Chocolate Brownie" to "Served warm, with chocolate sauce",
    "Baked Cheesecake" to "With berry compote",
    "Lemon Meringue Pie" to "Torched meringue on a buttery base",
    "Vanilla Ice Cream" to "Three scoops with chocolate sauce"
)

private val SIDES = course(
    "Sides", 25..65, Photos.RESTAURANT, listOf("Large", "Add cheese sauce", "Peri-peri salt", "Garlic aioli"),
    "Hand-Cut Chips" to "Sea salt and rosemary",
    "Sweet Potato Fries" to "With chipotle mayo",
    "Onion Rings" to "Beer-battered",
    "Side Salad" to "Leaves, cucumber and cherry tomatoes",
    "Creamed Spinach" to "With nutmeg and garlic",
    "Pap & Chakalaka" to "Stiff maize pap with a spicy vegetable relish",
    "Grilled Mielies" to "Two cobs with herb butter",
    "Coleslaw" to "Creamy, with apple and carrot"
)

private val PIZZAS = course(
    "Pizzas", 85..185, Photos.RESTAURANT,
    listOf("Extra mozzarella", "Gluten-free base", "Add bacon", "Add olives", "Add jalapeños", "Chilli oil"),
    "Margherita" to "Tomato, mozzarella and fresh basil",
    "Regina" to "Ham and mushroom",
    "Hawaiian" to "Ham and pineapple",
    "Bacon, Avo & Feta" to "The local favourite, with a balsamic drizzle",
    "Boerewors & Chakalaka" to "Spicy relish, red onion and mozzarella",
    "Peri-Peri Chicken" to "Chicken, peppers and peri-peri sauce",
    "Biltong & Avo" to "Shaved beef biltong, avocado and feta",
    "Four Cheese" to "Mozzarella, gorgonzola, feta and parmesan",
    "Vegetarian" to "Roast peppers, mushrooms, olives and onion"
)

private val BURGERS = course(
    "Burgers", 85..165, Photos.RESTAURANT,
    listOf("Add cheese", "Add bacon", "Add avo", "Add a fried egg", "Make it a double", "Gluten-free bun"),
    "Classic Beef Burger" to "Lettuce, tomato, pickles and house sauce",
    "Cheese Burger" to "Double cheddar and caramelised onion",
    "Bacon & Cheese Burger" to "Streaky bacon, cheddar and smoky mayo",
    "Mushroom Burger" to "With a creamy mushroom sauce",
    "Chilli Cheese Burger" to "Jalapeños, chilli jam and cheddar",
    "Chicken Burger" to "Crumbed breast, slaw and peri-peri mayo",
    "Veggie Burger" to "Chickpea patty, avo and tomato relish",
    "Boerewors Roll" to "Grilled boerewors with tomato and onion relish"
)

private val MILKSHAKES = course(
    "Milkshakes", 35..45, Photos.NONE, listOf("Large", "Extra thick", "Add whipped cream"),
    "Vanilla Milkshake" to "Real vanilla ice cream",
    "Chocolate Milkshake" to "Dark chocolate and cocoa",
    "Strawberry Milkshake" to "Made with fresh strawberries",
    "Peanut Butter Milkshake" to "With banana",
    "Bubblegum Milkshake" to "A South African favourite",
    "Coffee Milkshake" to "Espresso and vanilla"
)

private val FLAME_GRILLED = course(
    "Flame-Grilled Chicken", 65..195, Photos.RESTAURANT,
    listOf("Extra hot", "Lemon & herb basting", "Extra peri-peri sauce", "Add a roll"),
    "Quarter Chicken" to "Leg and thigh, basted as you like it",
    "Half Chicken" to "Basted with peri-peri or lemon and herb",
    "Whole Chicken" to "Feeds two to four",
    "Chicken Wings" to "Eight wings with an extra-hot basting",
    "Peri-Peri Chicken Livers" to "Pan-fried, with a Portuguese roll",
    "Chicken Espetada" to "A hanging skewer with garlic butter",
    "Chicken Prego Roll" to "Grilled fillet on a soft Portuguese roll",
    "Chicken Strips" to "Crumbed, with honey mustard"
)

private val PLATTERS = course(
    "Platters", 150..250, Photos.RESTAURANT, listOf("Extra wasabi", "Extra pickled ginger", "Spicy mayo", "Add avo"),
    "Mixed Platter" to "18 pieces of the chef's choice",
    "Salmon Platter" to "20 pieces of salmon nigiri, sashimi and maki",
    "Salmon & Tuna Platter" to "20 pieces of nigiri, sashimi and maki",
    "Vegetarian Platter" to "16 pieces with cucumber, avo and pickled radish",
    "Prawn Platter" to "Tempura prawn rolls and prawn nigiri",
    "Fashion Sandwich Platter" to "12 pieces of the Cape Town favourite",
    "Party Platter" to "40 pieces, made for sharing"
)

private val ROLLS = course(
    "Rolls", 65..130, Photos.RESTAURANT, listOf("Extra wasabi", "Spicy mayo", "Sweet chilli sauce", "Add avo"),
    "California Roll" to "Crab stick, avo, cucumber and sesame",
    "Salmon Rainbow Roll" to "A California roll topped with salmon",
    "Prawn Tempura Roll" to "Crispy prawn with spicy mayo",
    "Salmon Roses" to "Four pieces topped with mayo",
    "Salmon Fashion Sandwich" to "Four pieces, crumbed and fried",
    "Spicy Tuna Roll" to "Tuna, chilli mayo and spring onion",
    "Tuna Maki" to "Six pieces",
    "Cucumber Maki" to "Six pieces with sesame"
)

private val SASHIMI = course(
    "Sashimi", 85..160, Photos.RESTAURANT, listOf("Extra wasabi", "Extra pickled ginger", "Ponzu dressing"),
    "Salmon Sashimi" to "Six slices",
    "Tuna Sashimi" to "Six slices",
    "Salmon Nigiri" to "Four pieces",
    "Tuna Nigiri" to "Four pieces",
    "Seared Salmon Sashimi" to "Torched, with ponzu",
    "Tuna Tataki" to "Seared, with sesame and ponzu",
    "Sashimi Salad" to "Salmon and tuna on leaves, with a sesame dressing",
    "Salmon Tartare" to "Chopped salmon, avo and wasabi mayo"
)

private val INDIAN_STARTERS = course(
    "Starters", 45..95, Photos.RESTAURANT, listOf("Extra raita", "Mint chutney", "Make it hot"),
    "Vegetable Samoosas" to "Four, with mint chutney",
    "Mince Samoosas" to "Four, with spiced lamb mince",
    "Onion Bhaji" to "Crispy fritters with a tamarind dip",
    "Chilli Bites" to "Spiced fritters, a Durban favourite",
    "Chicken Tikka" to "Roasted in the tandoor, with raita",
    "Paneer Tikka" to "Grilled cottage cheese and peppers",
    "Lamb Seekh Kebab" to "Minced lamb skewers",
    "Papadums" to "With three chutneys"
)

private val INDIAN_CURRIES = course(
    "Curries", 95..175, Photos.RESTAURANT, listOf("Make it hot", "Add basmati rice", "Extra naan", "Add raita"),
    "Butter Chicken" to "Creamy tomato and fenugreek",
    "Lamb Rogan Josh" to "Slow-cooked with Kashmiri chilli",
    "Chicken Tikka Masala" to "Charred chicken in a spiced tomato sauce",
    "Durban Lamb Curry" to "Bone-in, with potato, and very hot",
    "Prawn Curry" to "Coconut, curry leaves and tamarind",
    "Lamb Vindaloo" to "Fiery, with potato",
    "Paneer Makhani" to "Cottage cheese in a buttery sauce",
    "Dhal Makhani" to "Black lentils, slow-cooked overnight"
)

private val BREADS_AND_RICE = course(
    "Breads & Rice", 25..55, Photos.RESTAURANT, listOf("Extra butter", "Add garlic", "Large"),
    "Butter Naan" to "From the tandoor",
    "Garlic Naan" to "With coriander",
    "Cheese Naan" to "Stuffed with cheddar",
    "Roti" to "Flaky and pan-fried",
    "Basmati Rice" to "Steamed",
    "Jeera Rice" to "With toasted cumin",
    "Pilau Rice" to "With whole spices",
    "Sambals" to "Tomato and onion, and cucumber raita"
)

private val DIM_SUM = course(
    "Dim Sum", 45..95, Photos.RESTAURANT, listOf("Chilli oil", "Extra soy sauce", "Black vinegar"),
    "Pork Dumplings" to "Six, steamed",
    "Prawn Har Gow" to "Four, in translucent wrappers",
    "Chicken Siu Mai" to "Four, open-topped",
    "Vegetable Spring Rolls" to "Four, with a sweet chilli dip",
    "Bao Buns" to "Two, with sticky pork belly",
    "Crispy Wontons" to "Six, with sweet chilli",
    "Prawn Toast" to "Sesame-crusted",
    "Salt & Pepper Squid" to "Crispy, with chilli and spring onion"
)

private val NOODLES = course(
    "Noodles", 85..145, Photos.RESTAURANT, listOf("Add egg", "Extra chilli", "Add prawns", "Swap to rice noodles"),
    "Chicken Chow Mein" to "Egg noodles, vegetables and soy",
    "Beef Chow Fun" to "Flat rice noodles, beef and bean sprouts",
    "Dan Dan Noodles" to "Spicy pork, sesame and chilli oil",
    "Singapore Noodles" to "Curried rice noodles, prawns and pork",
    "Vegetable Lo Mein" to "Soft noodles with seasonal vegetables",
    "Wonton Noodle Soup" to "Prawn wontons in a clear broth",
    "Prawn Noodle Soup" to "Clear broth with pak choi",
    "Sesame Noodles" to "Served cold, with cucumber and peanuts"
)

private val WOK = course(
    "From the Wok", 95..165, Photos.RESTAURANT,
    listOf("Add egg fried rice", "Extra chilli", "Add cashews", "Make it vegetarian"),
    "Sweet & Sour Pork" to "Pineapple and peppers",
    "Kung Pao Chicken" to "Peanuts and dried chillies",
    "Beef & Broccoli" to "In oyster sauce",
    "Black Bean Beef" to "Peppers and onion",
    "Lemon Chicken" to "Crispy, with a lemon glaze",
    "Mapo Tofu" to "Silken tofu with Sichuan pepper",
    "Salt & Pepper Prawns" to "Wok-tossed with garlic and chilli",
    "Egg Fried Rice" to "Spring onion and peas"
)

private val ANTIPASTI = course(
    "Antipasti", 55..125, Photos.RESTAURANT, listOf("Extra bread", "Add parmesan", "Gluten-free bread"),
    "Bruschetta" to "Tomato, basil, garlic and olive oil",
    "Arancini" to "Three, with mozzarella",
    "Calamari Fritti" to "With lemon aioli",
    "Burrata" to "Tomatoes and basil oil",
    "Beef Carpaccio" to "Rocket and parmesan",
    "Garlic Focaccia" to "Rosemary and sea salt",
    "Caprese Salad" to "Tomato, mozzarella and basil",
    "Antipasto Board" to "Cured meats, cheese and olives"
)

private val PASTA = course(
    "Pasta", 95..175, Photos.RESTAURANT, listOf("Extra parmesan", "Gluten-free pasta", "Add chilli", "Add chicken"),
    "Spaghetti Bolognese" to "Slow-cooked beef ragù",
    "Penne Arrabbiata" to "Tomato, garlic and chilli",
    "Fettuccine Alfredo" to "Parmesan cream",
    "Spaghetti Carbonara" to "Guanciale, egg yolk and pecorino",
    "Lasagne" to "Layers of ragù and béchamel",
    "Linguine alle Vongole" to "Clams, white wine and garlic",
    "Gnocchi Gorgonzola" to "Potato gnocchi in a blue cheese cream",
    "Butternut Ravioli" to "Sage butter and toasted pine nuts"
)

private val DOLCI = course(
    "Dolci", 45..95, Photos.DESSERT, listOf("Add vanilla gelato", "Add whipped cream", "Extra espresso"),
    "Tiramisu" to "Espresso, mascarpone and cocoa",
    "Panna Cotta" to "Vanilla, with a berry coulis",
    "Cannoli" to "Two, with ricotta and pistachio",
    "Affogato" to "Gelato drowned in espresso",
    "Chocolate Fondant" to "With a molten centre",
    "Lemon Tart" to "Torched meringue",
    "Gelato" to "Three scoops"
)

private val GRILL_STARTERS = course(
    "Starters", 55..110, Photos.RESTAURANT, listOf("Extra lemon", "Add garlic bread", "Make it a main"),
    "Grilled Halloumi" to "Citrus glaze, micro herbs",
    "Peri-Peri Chicken Livers" to "Creamy peri-peri sauce and toasted ciabatta",
    "Biltong Platter" to "Beef biltong and droëwors",
    "Calamari Strips" to "Lemon butter or peri-peri",
    "Garlic Snails" to "Six, baked in garlic butter",
    "Pork Belly Bites" to "With a sticky soy glaze",
    "Boerewors Bites" to "Grilled, with a chakalaka dip",
    "Prawn Cocktail" to "With Marie Rose sauce"
)

private val STEAKS = course(
    "Steaks", 165..250, Photos.RESTAURANT,
    listOf("Pepper sauce", "Mushroom sauce", "Cheese sauce", "Garlic butter", "Add prawns"),
    "Rump 300 g" to "Aged for 21 days and basted",
    "Sirloin 300 g" to "Aged, with its fat cap",
    "Fillet 250 g" to "The leanest, most tender cut",
    "Ribeye 350 g" to "Well marbled",
    "T-Bone 500 g" to "Sirloin and fillet on the bone",
    "Wagyu Rump 250 g" to "Rich and buttery",
    "Pork Ribs" to "A full rack with a sticky basting",
    "Lamb Chops" to "Three, with rosemary and garlic"
)

private val SEAFOOD_STARTERS = course(
    "Starters", 55..110, Photos.RESTAURANT, listOf("Extra lemon", "Tartare sauce", "Peri-peri sauce"),
    "Calamari" to "Tubes and tentacles in lemon butter",
    "Mussels" to "White wine, garlic and cream",
    "Oysters" to "Half a dozen, fresh from the West Coast",
    "Fish Cakes" to "Two, with tartare sauce",
    "Smoked Snoek Pâté" to "With toast",
    "Peri-Peri Prawns" to "Six, sizzling in garlic butter",
    "Seafood Chowder" to "Creamy, with mussels and hake",
    "Prawn Cocktail" to "With Marie Rose sauce"
)

private val FISH_AND_CHIPS = course(
    "Fish & Chips", 85..165, Photos.RESTAURANT,
    listOf("Large chips", "Tartare sauce", "Extra lemon", "Swap chips for salad"),
    "Hake & Chips" to "Beer-battered or grilled",
    "Snoek & Chips" to "Grilled with an apricot glaze",
    "Calamari & Chips" to "In lemon butter",
    "Kingklip & Chips" to "Grilled, with lemon butter",
    "Yellowtail & Chips" to "Line fish, grilled",
    "Hake Goujons & Chips" to "Crumbed strips with tartare sauce",
    "Prawns & Chips" to "Six queen prawns",
    "Fish Burger" to "Crumbed hake, tartare and lettuce"
)

private val SEAFOOD_GRILLS = course(
    "Grills & Platters", 145..250, Photos.RESTAURANT,
    listOf("Garlic butter", "Lemon butter", "Peri-peri sauce", "Add calamari"),
    "Seafood Platter for One" to "Hake, calamari, prawns and mussels",
    "Seafood Platter for Two" to "Line fish, calamari, prawns and mussels",
    "Queen Prawns" to "Ten, grilled",
    "Grilled Line Fish" to "The catch of the day",
    "Grilled Kingklip" to "Lemon butter and seasonal vegetables",
    "Seafood Paella" to "Saffron rice, mussels, prawns and calamari",
    "Cape Malay Fish Curry" to "Hake in a fragrant curry, with sambals",
    "Grilled Salmon" to "With a teriyaki glaze"
)

private val BREAKFAST = course(
    "Breakfast", 55..125, Photos.RESTAURANT,
    listOf("Add bacon", "Add avo", "Extra egg", "Gluten-free toast", "Swap to rye"),
    "Full Breakfast" to "Eggs, bacon, boerewors, mushrooms, tomato and toast",
    "Eggs Benedict" to "Poached eggs, ham and hollandaise",
    "Avo on Toast" to "Sourdough, feta, chilli and lemon",
    "Shakshuka" to "Eggs baked in a spiced tomato sauce",
    "Flapjacks" to "Three, with bacon and syrup",
    "Mince on Toast" to "Savoury mince with a poached egg",
    "French Toast" to "Brioche, berries and mascarpone",
    "Granola Bowl" to "Yoghurt, berries and honey"
)

private val LIGHT_MEALS = course(
    "Light Meals", 75..135, Photos.RESTAURANT,
    listOf("Add chips", "Add a side salad", "Gluten-free bread", "Add avo"),
    "Chicken Mayo Toastie" to "On sourdough",
    "Cheese & Tomato Toastie" to "Melted cheddar and tomato",
    "Club Sandwich" to "Chicken, bacon and egg in three layers",
    "Chicken Caesar Salad" to "Cos, parmesan and croutons",
    "Quiche of the Day" to "With a side salad",
    "Bobotie Pie" to "Spiced mince in puff pastry",
    "Soup of the Day" to "With freshly baked bread",
    "Halloumi Wrap" to "Hummus, rocket and roasted peppers"
)

private val BAKERY = course(
    "Bakery", 25..65, Photos.DESSERT, listOf("Warmed", "Add butter and jam", "Add cream"),
    "Croissant" to "All butter",
    "Pain au Chocolat" to "Dark chocolate",
    "Blueberry Muffin" to "Baked this morning",
    "Carrot Cake" to "With cream cheese icing",
    "Scones" to "Two, with jam and cream",
    "Milk Tart Slice" to "Cinnamon custard on crisp pastry",
    "Cinnamon Bun" to "With a cream cheese glaze",
    "Rusks" to "Buttermilk, two per portion"
)

private val COFFEE_AND_TEA = course(
    "Coffee & Tea", 20..45, Photos.NONE, listOf("Oat milk", "Almond milk", "Extra shot", "Decaf", "Large"),
    "Espresso" to "A double shot",
    "Americano" to "Espresso and hot water",
    "Cappuccino" to "With velvety milk foam",
    "Flat White" to "A double shot with steamed milk",
    "Café Latte" to "Mild and milky",
    "Red Cappuccino" to "Rooibos espresso with foamed milk",
    "Rooibos Tea" to "With honey and lemon",
    "Chai Latte" to "Spiced and sweetened",
    "Hot Chocolate" to "Rich, with marshmallows"
)

private val BOWLS = course(
    "Bowls", 85..135, Photos.RESTAURANT,
    listOf("Add avo", "Add halloumi", "Add chicken", "Extra dressing", "Swap to quinoa"),
    "Poke Bowl" to "Salmon, sushi rice, edamame and avo",
    "Buddha Bowl" to "Roast sweet potato, chickpeas and tahini",
    "Chicken Quinoa Bowl" to "Grilled chicken, quinoa and greens",
    "Falafel Bowl" to "Hummus, tabbouleh and pickles",
    "Teriyaki Tofu Bowl" to "Brown rice, broccoli and sesame",
    "Açaí Bowl" to "Banana, berries and granola",
    "Burrito Bowl" to "Black beans, corn, salsa and brown rice",
    "Harvest Bowl" to "Roast butternut, lentils, spinach and feta"
)

private val SALADS = course(
    "Salads", 75..125, Photos.RESTAURANT, listOf("Add chicken", "Add avo", "Add feta", "Dressing on the side"),
    "Greek Salad" to "Feta, olives, cucumber and red onion",
    "Chicken & Avo Salad" to "Leaves, cherry tomatoes and seeds",
    "Butternut & Feta Salad" to "Roasted butternut, rocket and pumpkin seeds",
    "Quinoa Salad" to "Herbs, pomegranate and lemon",
    "Caesar Salad" to "Cos, parmesan and croutons",
    "Beetroot & Goat's Cheese" to "Walnuts and a honey dressing",
    "Asian Slaw" to "Cabbage and carrot with a sesame-ginger dressing",
    "Halloumi Salad" to "Grilled halloumi and roasted peppers"
)

private val SMOOTHIES = course(
    "Smoothies & Juices", 35..45, Photos.NONE, listOf("Add protein", "Add peanut butter", "Oat milk", "Large"),
    "Green Smoothie" to "Spinach, apple, cucumber and ginger",
    "Berry Smoothie" to "Mixed berries, banana and yoghurt",
    "Mango Smoothie" to "Mango, granadilla and coconut",
    "Peanut Butter Smoothie" to "Banana, oats and dates",
    "Cold-Pressed Juice" to "Apple, carrot and ginger",
    "Fresh Orange Juice" to "Squeezed to order",
    "Iced Matcha" to "With oat milk"
)

private val TACOS = course(
    "Tacos", 65..125, Photos.RESTAURANT,
    listOf("Extra salsa", "Add guacamole", "Add jalapeños", "Extra hot sauce", "Corn tortillas"),
    "Beef Barbacoa Tacos" to "Three, with slow-cooked beef and pickled onion",
    "Chicken Tinga Tacos" to "Three, with chipotle chicken",
    "Fish Tacos" to "Three, with crispy hake and lime crema",
    "Carnitas Tacos" to "Three, with pulled pork and salsa verde",
    "Mushroom Tacos" to "Three, with chipotle mushrooms and feta",
    "Prawn Tacos" to "Three, with garlic prawns and mango salsa",
    "Al Pastor Tacos" to "Three, with pork and pineapple",
    "Nachos" to "Cheese, jalapeños, guacamole and salsa"
)

private val BURRITOS = course(
    "Burritos & Bowls", 95..155, Photos.RESTAURANT,
    listOf("Add guacamole", "Extra cheese", "Add jalapeños", "Make it a bowl"),
    "Chicken Burrito" to "Rice, black beans, cheese and salsa",
    "Beef Burrito" to "Shredded beef, beans and cheese",
    "Bean Burrito" to "Refried beans, rice and guacamole",
    "Pulled Pork Burrito" to "Pickled onion and chipotle mayo",
    "Chicken Quesadilla" to "Cheese, peppers and sour cream",
    "Burrito Bowl" to "Rice, beans, corn and salsa",
    "Enchiladas" to "Two, baked in a red sauce",
    "Chimichanga" to "A fried burrito with beef and beans"
)

private val THAI_STARTERS = course(
    "Starters", 55..105, Photos.RESTAURANT, listOf("Extra sweet chilli", "Add peanut sauce", "Make it spicy"),
    "Chicken Satay" to "Four skewers with peanut sauce",
    "Spring Rolls" to "Four, with sweet chilli",
    "Tom Yum Soup" to "Hot and sour, with prawns",
    "Tom Kha Gai" to "Coconut chicken soup with galangal",
    "Thai Fish Cakes" to "With a cucumber relish",
    "Prawn Crackers" to "With sweet chilli",
    "Som Tam" to "Green papaya salad with peanuts and lime",
    "Larb Gai" to "Spicy minced chicken salad with mint"
)

private val THAI_CURRIES = course(
    "Curries", 105..165, Photos.RESTAURANT,
    listOf("Add jasmine rice", "Extra hot", "Add prawns", "Make it vegetarian"),
    "Green Curry" to "Chicken, Thai basil and aubergine",
    "Red Curry" to "Beef, bamboo shoots and makrut lime",
    "Massaman Curry" to "Lamb, potato and peanuts",
    "Panang Curry" to "Chicken in a thick peanut sauce",
    "Yellow Curry" to "Chicken, potato and turmeric",
    "Jungle Curry" to "Vegetables, no coconut, very hot",
    "Prawn Red Curry" to "Coconut and cherry tomatoes",
    "Tofu Green Curry" to "Vegetables and Thai basil"
)

private val THAI_NOODLES = course(
    "Noodles & Stir-Fries", 95..155, Photos.RESTAURANT,
    listOf("Add egg", "Extra peanuts", "Add prawns", "Extra chilli"),
    "Pad Thai" to "Rice noodles, tamarind, peanuts and egg",
    "Pad See Ew" to "Wide noodles, soy and Chinese broccoli",
    "Drunken Noodles" to "Holy basil, chilli and wide noodles",
    "Thai Fried Rice" to "Egg, onion and lime",
    "Cashew Chicken" to "Stir-fried with dried chilli",
    "Pad Kra Pao" to "Minced pork, holy basil and a fried egg",
    "Khao Soi" to "Curry noodle soup with crispy noodles",
    "Glass Noodle Stir-Fry" to "Prawns, cabbage and egg"
)

private val MEZZE = course(
    "Mezze", 45..95, Photos.RESTAURANT, listOf("Extra pita", "Add feta", "Extra tahini"),
    "Hummus" to "Chickpeas, tahini and olive oil, with pita",
    "Baba Ganoush" to "Smoky aubergine and pomegranate",
    "Falafel" to "Six, with tahini",
    "Tzatziki" to "Yoghurt, cucumber and garlic, with pita",
    "Dolmades" to "Vine leaves stuffed with rice",
    "Halloumi Fries" to "Za'atar and honey",
    "Spanakopita" to "Spinach and feta pastry",
    "Tabbouleh" to "Parsley, bulgur and lemon"
)

private val SHAWARMA = course(
    "Shawarma & Wraps", 75..125, Photos.RESTAURANT,
    listOf("Extra garlic sauce", "Add chips inside", "Add halloumi", "Make it spicy"),
    "Chicken Shawarma" to "Garlic sauce and pickles in pita",
    "Lamb Shawarma" to "Tahini, tomato and onion",
    "Falafel Wrap" to "Hummus, salad and pickled turnip",
    "Lamb Souvlaki Pita" to "Tzatziki, tomato and onion",
    "Chicken Gyro" to "Tzatziki and chips in pita",
    "Halloumi Wrap" to "Grilled halloumi and roasted peppers",
    "Beef Kofta Wrap" to "Spiced mince and a yoghurt sauce",
    "Mixed Shawarma Plate" to "Chicken and lamb with rice and salad"
)

private val GRILLS = course(
    "Grills", 125..220, Photos.RESTAURANT, listOf("Add rice", "Extra pita", "Garlic sauce", "Chilli sauce"),
    "Mixed Grill" to "Lamb, chicken and kofta, with rice",
    "Lamb Chops" to "Lemon and oregano",
    "Chicken Kebab Plate" to "Two skewers with rice and salad",
    "Lamb Kofta Plate" to "Three kofta with tahini and salad",
    "Moussaka" to "Aubergine, lamb mince and béchamel",
    "Grilled Calamari" to "Lemon and oregano",
    "Lamb Kleftiko" to "Slow-roasted, with potatoes",
    "Beef Kebab Plate" to "Two skewers with rice and salad"
)

private val SWEETS = course(
    "Sweets", 35..75, Photos.DESSERT, listOf("Add ice cream", "Extra syrup", "Add pistachios"),
    "Baklava" to "Filo, walnuts and honey syrup",
    "Kunafa" to "Cheese pastry with rose syrup",
    "Loukoumades" to "Honey doughnuts with cinnamon",
    "Galaktoboureko" to "Semolina custard in filo",
    "Turkish Delight" to "Rose and lemon",
    "Rice Pudding" to "Rose water and pistachio"
)

private val CAPE_MALAY_CURRIES = course(
    "Curries & Bredies", 95..165, Photos.RESTAURANT,
    listOf("Extra roti", "Add sambals", "Make it hot", "Add rice"),
    "Cape Malay Chicken Curry" to "Mild and fragrant, with potato",
    "Lamb Curry" to "Bone-in and slow-cooked",
    "Bobotie" to "Spiced mince under a baked egg custard, with yellow rice",
    "Tomato Bredie" to "Lamb stewed with tomato and cinnamon",
    "Denningvleis" to "Sweet-sour lamb with tamarind and bay",
    "Butter Bean Curry" to "Vegetarian, with tomato",
    "Fish Curry" to "Hake, tomato and curry leaves",
    "Waterblommetjie Bredie" to "Lamb with Cape water lilies"
)

private val BREYANI = course(
    "Breyani", 95..165, Photos.RESTAURANT, listOf("Add sambals", "Add a boiled egg", "Extra lentils", "Make it hot"),
    "Chicken Breyani" to "Basmati, lentils, potato and fried onion",
    "Lamb Breyani" to "Slow-cooked, with lentils and potato",
    "Mutton Breyani" to "The Sunday favourite",
    "Prawn Breyani" to "With saffron and fried onion",
    "Vegetable Breyani" to "Seasonal vegetables and lentils",
    "Chicken Akhni" to "A lighter rice dish with chicken and potato"
)

private val GATSBYS = course(
    "Gatsbys & Rotis", 65..150, Photos.RESTAURANT, listOf("Add egg", "Add cheese", "Extra chips", "Add chilli sauce"),
    "Masala Steak Gatsby" to "Steak, chips, lettuce and masala sauce",
    "Slap Chip Gatsby" to "Soft chips with vinegar and peri-peri",
    "Chicken Gatsby" to "Grilled chicken, chips and salad",
    "Calamari Gatsby" to "Fried calamari, chips and sauce",
    "Bunny Chow" to "A quarter loaf filled with mutton curry",
    "Chicken Salomie" to "Curry wrapped in a roti",
    "Mutton Roti" to "Curry in a flaky roti",
    "Dhaltjies" to "Chilli bites, six per portion"
)

private val CAKES = course(
    "Cakes & Tarts", 35..85, Photos.DESSERT, listOf("Add cream", "Add ice cream", "Warmed"),
    "Red Velvet Cake" to "Cream cheese frosting",
    "Chocolate Cake" to "Three layers with ganache",
    "Carrot Cake" to "With cream cheese icing",
    "Lemon Meringue Tart" to "Torched meringue",
    "Milk Tart" to "Cinnamon custard",
    "Cheesecake" to "New York style, with berry compote",
    "Pecan Pie" to "Caramel and toasted pecans",
    "Apple Crumble" to "Cinnamon, with custard"
)

private val WAFFLES = course(
    "Waffles & Crêpes", 55..110, Photos.DESSERT, listOf("Add ice cream", "Add banana", "Extra syrup", "Add berries"),
    "Belgian Waffle" to "Maple syrup and cream",
    "Chocolate Waffle" to "Chocolate sauce and banana",
    "Berry Waffle" to "Mixed berries and mascarpone",
    "Bacon & Syrup Waffle" to "Streaky bacon and maple syrup",
    "Lemon & Sugar Crêpe" to "The classic",
    "Chocolate & Banana Crêpe" to "Served warm, with cream",
    "Salted Caramel Crêpe" to "With toasted nuts",
    "Cinnamon Sugar Crêpe" to "With vanilla ice cream"
)

private val ICE_CREAM = course(
    "Ice Cream", 35..75, Photos.DESSERT, listOf("Add a cone", "Extra scoop", "Add sprinkles", "Chocolate sauce"),
    "Vanilla Bean" to "Two scoops",
    "Dark Chocolate" to "Two scoops",
    "Salted Caramel" to "Two scoops",
    "Rooibos & Honey" to "Two scoops, a local favourite",
    "Strawberry Sorbet" to "Dairy-free",
    "Mango Sorbet" to "Dairy-free",
    "Ice Cream Sundae" to "Three scoops with sauce and nuts",
    "Banana Split" to "Three scoops and chocolate sauce"
)
