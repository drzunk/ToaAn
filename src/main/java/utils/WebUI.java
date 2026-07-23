package utils;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.SkipException;

import java.io.File;
import java.time.Duration;
import java.util.List;

public class WebUI {
    private WebDriver driver;
    private WebDriverWait wait;

    public WebUI(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void scrollToElement(By by) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(300);
        } catch (Exception e) {
        }
    }

    public void clickElement(By by, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            try {
                wait.until(ExpectedConditions.elementToBeClickable(by)).click();
            } catch (Exception ex) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].click();", element);
            }
            // CHỈ IN RA CONSOLE, KHÔNG IN RA REPORT HTML NỮA
            System.out.println(" ➔ Click vào: [" + elementName + "]");
        } catch (Exception e) {
            throw new SkipException("❌ Lỗi: Không tìm thấy hoặc không thể click: [" + elementName + "]");
        }
    }

    public void setText(By by, String value, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            element.clear();
            element.sendKeys(value);
            // CHỈ IN RA CONSOLE
            System.out.println(" ➔ Điền: '" + value + "' vào [" + elementName + "]");
        } catch (Exception e) {
            throw new SkipException("❌ Lỗi: Không tìm thấy ô [" + elementName + "] để điền '" + value + "'");
        }
    }

    public boolean isElementVisible(By by) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement element = shortWait.until(ExpectedConditions.presenceOfElementLocated(by));
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isElementEnabled(By by) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement element = shortWait.until(ExpectedConditions.presenceOfElementLocated(by));
            return element.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public void setTextWithCheck(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do Excel trống.");
            return;
        }
        if (!isElementVisible(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do form ẩn.");
            return;
        }
        if (!isElementEnabled(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do hệ thống khóa.");
            return;
        }
        setText(by, value, elementName);
    }

    public void setTextForMaskedInput(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do Excel trống.");
            return;
        }
        if (!isElementVisible(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do form ẩn.");
            return;
        }
        if (!isElementEnabled(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do hệ thống khóa.");
            return;
        }

        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            element.sendKeys(value);
            System.out.println(" ➔ Điền (Mask): '" + value + "' vào [" + elementName + "]");
        } catch (Exception e) {
            throw new SkipException("❌ Lỗi: Không thể nhập dữ liệu vào ô [" + elementName + "]");
        }
    }

    public void selectCustomDropdown(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
        clickElement(dropdownLocator, elementName);
        sleep(1);
        boolean isFound = false;
        try {
            List<WebElement> options = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(optionsLocator));
            for (WebElement option : options) {
                String textOnWeb = option.getText();
                if (textOnWeb == null || textOnWeb.trim().isEmpty()) {
                    textOnWeb = option.getAttribute("textContent");
                }
                if (textOnWeb != null && textOnWeb.trim().toLowerCase().contains(expectedText.trim().toLowerCase())) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", option);
                    Thread.sleep(300);
                    option.click();
                    System.out.println(" ➔ Chọn Dropdown: '" + expectedText + "' tại [" + elementName + "]");
                    isFound = true;
                    break;
                }
            }
        } catch (Exception e) {
        }
        if (!isFound) {
            throw new SkipException("❌ Lỗi dữ liệu/Web lag: Không tìm thấy ['" + expectedText + "'] trong [" + elementName + "]");
        }
    }

    public void selectDropdownWithSearch(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        clickElement(dropdownLocator, elementName);
        sleep(1);
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInputLocator));
            searchInput.clear();
            searchInput.sendKeys(expectedText);
            System.out.println(" ➔ Gõ tìm kiếm Dropdown: '" + expectedText + "'");
            Thread.sleep(1000);
        } catch (Exception e) {
        }

        boolean isFound = false;
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            List<WebElement> options = shortWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(optionsLocator));
            for (WebElement option : options) {
                if (option.getText().trim().toLowerCase().contains(expectedText.trim().toLowerCase())) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", option);
                    option.click();
                    System.out.println(" ➔ Chọn Dropdown lọc: '" + expectedText + "' tại [" + elementName + "]");
                    isFound = true;
                    break;
                }
            }
        } catch (Exception e) {
        }

        if (!isFound) {
            throw new SkipException("❌ Lỗi dữ liệu: Không tìm thấy ['" + expectedText + "'] trong Dropdown [" + elementName + "]");
        }
    }

    public void selectDropdownWithCheck(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua Dropdown: [" + elementName + "] do Excel trống.");
            return;
        }
        if (!isElementVisible(dropdownLocator)) {
            System.out.println(" ⏩ Bỏ qua Dropdown: [" + elementName + "] do form ẩn.");
            return;
        }
        selectCustomDropdown(dropdownLocator, optionsLocator, expectedText, elementName);
    }

    public void selectToaAnWithCheck(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua Tòa án: [" + elementName + "] do Excel trống.");
            return;
        }
        if (!isElementVisible(dropdownLocator)) {
            System.out.println(" ⏩ Bỏ qua Tòa án: [" + elementName + "] do form ẩn.");
            return;
        }
        selectDropdownWithSearch(dropdownLocator, searchInputLocator, optionsLocator, expectedText, elementName);
    }

    public void zoomPage(String percentage) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("document.body.style.zoom = '" + percentage + "'");
            System.out.println(" ➔ Thu nhỏ màn hình: " + percentage);
        } catch (Exception e) {
        }
    }

    public void captureScreen(String message) {
        try {
            org.openqa.selenium.TakesScreenshot ts = (org.openqa.selenium.TakesScreenshot) driver;
            String base64Image = ts.getScreenshotAs(org.openqa.selenium.OutputType.BASE64);
            // DUY NHẤT LỆNH NÀY ĐƯỢC GIỮ LẠI ĐỂ IN ẢNH RA BÁO CÁO HTML
            ExtentReportManager.logPassWithScreenshot(message, base64Image);
        } catch (Exception e) {
        }
    }

    public void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
        }
    }

    public String docCaptcha(By locatorAnhCaptcha) {
        String result = "";
        try {
            WebElement captchaElement = driver.findElement(locatorAnhCaptcha);
            String rawText = captchaElement.getText();
            if (rawText != null && !rawText.trim().isEmpty()) {
                result = rawText.replaceAll("[^a-zA-Z0-9]", "");
                System.out.println(" 🤖 Bắt Captcha từ HTML: " + result);
                return result;
            }

            File srcFile = captchaElement.getScreenshotAs(OutputType.FILE);
            ITesseract tesseract = new Tesseract();
            String absoluteTessDataPath = System.getProperty("user.dir") + java.io.File.separator + "tessdata";
            tesseract.setDatapath(absoluteTessDataPath);
            tesseract.setLanguage("eng");

            result = tesseract.doOCR(srcFile);
            result = result.replaceAll("[^a-zA-Z0-9]", "");
            System.out.println(" 🤖 AI giải mã Captcha thành: " + result);

        } catch (Exception e) {
            System.out.println(" ❌ Lỗi đọc Captcha: " + e.getMessage());
        }
        return result;
    }
}