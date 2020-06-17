package com.testpros;

import com.testpros.fast.By;
import com.testpros.fast.WebDriver;
import com.testpros.fast.WebElement;
import org.junit.Test;

import java.util.List;

public class SampleIT extends TestBase {

    @Test
    public void recentNewsTest() {
        assertElementDisplayed(By.tagName("main"));
    }

    @Test
    public void recentNewsHeaderTest() {
        WebDriver driver = drivers.get();
        WebElement header = driver.findElement(By.tagName("main")).findElement(By.tagName("h1"));
        assertElementDisplayed(driver.findElement(By.tagName("main")).findElement(By.tagName("h1")));
        assertElementTextEquals("Recent News", header);
    }

    @Test
    public void recentNewsThreeItems() {
        WebDriver driver = drivers.get();
        WebElement postsContainer = driver.findElement(By.tagName("main")).findElement(
                By.className("elementor-posts-container"));
        List<WebElement> articles = postsContainer.findElements(By.tagName("article"));
        assertEquals(articles.size(), 3, "Expected to find '3' articles",
                "Actually found '" + articles.size() + "'");
    }
}
