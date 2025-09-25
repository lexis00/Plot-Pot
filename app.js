// Select input and submit button
const locationInput = document.getElementById('location-input');
const locationSubmit = document.getElementById('location-submit');

// Select sections for toggling
const headerSection = document.querySelector('.header'); 
const flashcardSection = document.getElementById('flashcard-section');
const flashcardContainer = document.getElementById('flashcard-container');
const zonesection = document.querySelector('.zones-section'); 

locationSubmit.addEventListener('click', async () => {
    const userInput = locationInput.value.trim();
    if (!userInput) {
        alert("Please enter a city or zip code.");
        return;
    }

    try {
        // Hide the welcome and zone sections
        headerSection.style.display = "none";
        zonesection.style.display = "none";

        // Show the flashcard section
        flashcardSection.style.display = "block";

        // 1. Fetch location/weather data
        const locationResponse = await fetch(`http://localhost:8080/location?input=${encodeURIComponent(userInput)}`);
        if (!locationResponse.ok) throw new Error(`HTTP error! Status: ${locationResponse.status}`);
        const locationData = await locationResponse.json();
        console.log("Location response:", locationData);

        const tempF = locationData["temperature in fahrenheit"] || 70; // fallback if missing

        // 2. Fetch plant data
        const plantResponse = await fetch(`http://localhost:8080/plants?location=${encodeURIComponent(userInput)}`);
        if (!plantResponse.ok) throw new Error(`HTTP error! Status: ${plantResponse.status}`);
        const plantData = await plantResponse.json();
        console.log("Plant data:", plantData);

        // Clear previous cards
        flashcardContainer.innerHTML = "";
        let currentIndex = 0; // track which card is showing
        let cards = [];       // store generated cards

        function renderCard(index) {
            flashcardContainer.innerHTML = ""; // clear container
            if (cards.length > 0 && index >= 0 && index < cards.length) {
                flashcardContainer.appendChild(cards[index]);
            }
        }

        // 3. Dynamically create flashcards
        if (plantData.plants && plantData.plants.length > 0) {
            plantData.plants.forEach(plant => {
                const card = document.createElement("div");
                card.className = "flashcard";
                card.style.background = "white";
                card.style.border = "1px solid #ddd";
                card.style.borderRadius = "16px";
                card.style.padding = "16px";
                card.style.margin = "10px auto";
                card.style.width = "500px";
                card.style.height = "250px"
                card.style.textAlign = "center";
                card.style.boxShadow = "0 2px 6px rgba(0,0,0,0.1)";

                // Watering advice
                let wateringAdvice;
                if (plant.watering && plant.watering !== "N/A") {
                    wateringAdvice = plant.watering;
                } else {
                    if (tempF > 80) {
                        wateringAdvice = "High";
                    } else if (tempF > 65) {
                        wateringAdvice = "Moderate";
                    } else {
                        wateringAdvice = "Low";
                    }
                }

                card.innerHTML = `
                    <h3>${plant.plantName}</h3>
                    <p><strong>Water Intake:</strong> ${wateringAdvice}</p>
                `;

                cards.push(card); // store card for carousel
            });

            // Render first card immediately
            renderCard(currentIndex);
        } else {
            flashcardContainer.innerHTML = `<p style="color:red; text-align:center;">No plants found for this location.</p>`;
        }

        // 4. Next / Prev button handlers
        document.getElementById("prev-btn").onclick = () => {
            if (currentIndex > 0) {
                currentIndex--;
                renderCard(currentIndex);
            }
        };

        document.getElementById("next-btn").onclick = () => {
            if (currentIndex < cards.length - 1) {
                currentIndex++;
                renderCard(currentIndex);
            }
        };

    } catch (error) {
        console.error("Error fetching location or plant data:", error);
        flashcardContainer.innerHTML = `<p style="color:red; text-align:center;">Error fetching data. Please try again.</p>`;
    }
});
