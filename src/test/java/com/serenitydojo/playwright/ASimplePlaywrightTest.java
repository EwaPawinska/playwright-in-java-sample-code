package com.serenitydojo.playwright;


import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UsePlaywright(HeadlessChromeOptions.class)
public class ASimplePlaywrightTest {

    @Test
    void shouldShowThePageTitle(Page page) {
        page.navigate("https://practicesoftwaretesting.com");
        String title = page.title();
        assertTrue(title.contains("Practice Software Testing"));
    }

    @Test
    void shouldShowSearchTermsInTheTitle(Page page) {
        page.navigate("https://practicesoftwaretesting.com");
        page.locator("[placeholder=Search]").fill("Pliers");
        page.locator("button:has-text('Search')").click();

        assertThat(page.locator(".card-title")).not().hasCount(0);
    }

    @DisplayName("Search for pliers")
    @Test
    void shouldFindFourPliersResults(Page page) {
        page.navigate("https://practicesoftwaretesting.com");
        page.getByPlaceholder("Search").fill("Pliers");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search")).click();

        assertThat(page.locator(".card")).hasCount(4);
        List<String> productNames = page.getByTestId("product-name").allTextContents();
        Assertions.assertThat(productNames).allMatch(name -> name.contains("Pliers"));

        Locator outOfStockItem = page.locator(".card")
                .filter(new Locator.FilterOptions().setHasText("Out of stock"))
                .getByTestId("product-name");
        assertThat(outOfStockItem).hasCount(1);
        assertThat(outOfStockItem).hasText("Long Nose Pliers");
    }

    @DisplayName("Complete the form")
    @Test
    public void shouldDisplayContactForm(Page page) throws URISyntaxException {
        page.navigate("https://practicesoftwaretesting.com/contact");

        Locator firstNameInput = page.getByLabel("First name");
        Locator lastName = page.getByLabel("Last name");
        Locator email = page.getByLabel("Email");
        Locator messageField = page.getByLabel("Message");
        Locator subjectField = page.getByLabel("Subject");
        Locator uploadField = page.getByLabel("Attachment");

        firstNameInput.fill("Sarah-Jane");
        lastName.fill("Jones");
        email.fill("sarah.jones@gmail.com");
        messageField.fill("Some message");
        subjectField.selectOption("Warranty");

        Path fileToUpload = Paths.get(ClassLoader.getSystemResource("data/example.txt").toURI());

        page.setInputFiles("#attachment", fileToUpload);

        assertThat(firstNameInput).hasValue("Sarah-Jane");
        assertThat(lastName).hasValue("Jones");
        assertThat(email).hasValue("sarah.jones@gmail.com");
        assertThat(messageField).hasValue("Some message");
        assertThat(subjectField).hasValue("warranty");

        String uploadedFile = uploadField.inputValue();
        org.assertj.core.api.Assertions.assertThat(uploadedFile).endsWith("example.txt");
    }

    @DisplayName("Mandatory fields")
    @ParameterizedTest
    @ValueSource(strings = {"First name", "Last name", "Email", "Message"})
    public void verifyMandatoryFields(String fieldName, Page page) {
        page.navigate("https://practicesoftwaretesting.com/contact");

        Locator firstNameInput = page.getByLabel("First name");
        Locator lastName = page.getByLabel("Last name");
        Locator email = page.getByLabel("Email");
        Locator messageField = page.getByLabel("Message");
        Locator subjectField = page.getByLabel("Subject");
        Locator uploadField = page.getByLabel("Attachment");
        Locator sendButton = page.getByText("Send");

        //Fill in the fields values
        firstNameInput.fill("Sarah-Jane");
        lastName.fill("Jones");
        email.fill("sarah.jones@gmail.com");
        messageField.fill("Some message");
        subjectField.selectOption("Warranty");

        // Clear one of the fields
        page.getByLabel(fieldName).clear();

        sendButton.click();

        Locator errorMessage = page.getByRole(AriaRole.ALERT).getByText("%s is required".formatted(fieldName));
        assertThat(errorMessage).isVisible();

        assertThat(subjectField).not().isVisible();
    }

    @Test
    void allProductPRicesShouldHaveCorrectValues(Page page) {
        page.navigate("https://practicesoftwaretesting.com");

        List<Double> prices = page.getByTestId("product-price")
                .allInnerTexts()
                .stream()
                .map(price -> Double.parseDouble(price.replace("$", "")))
                .toList();

        Assertions.assertThat(prices)
                .isNotEmpty()
                .allMatch(price -> price > 0)
                .doesNotContain(0.0)
                .allMatch(price -> price < 1000)
                .allSatisfy(price ->
                        Assertions.assertThat(price)
                                .isGreaterThan(0.0)
                                .isLessThan(1000.0));
    }

    @Test
    void shouldSortInAlphabeticalOrder(Page page) {
        page.navigate("https://practicesoftwaretesting.com");

        page.getByLabel("Sort").selectOption("Name (Z - A)");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        List<String> productNames = page.getByTestId("product-name").allTextContents();

//        Assertions.assertThat(productNames).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        Assertions.assertThat(productNames).isSortedAccordingTo(Comparator.reverseOrder());
    }
}
