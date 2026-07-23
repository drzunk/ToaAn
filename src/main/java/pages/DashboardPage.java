package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WebUI;

public class DashboardPage {
    private WebDriver driver;
    private WebUI webUI;

    // 1. Khai báo Locators trên màn hình Dashboard
    private By btnNopDonMoi = By.xpath("//a[contains(., 'Nộp đơn mới')]");

    // 2. Constructor
    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    // 3. Các hàm thao tác (Nghiệp vụ)
    public void clickNopDonMoi() {
        webUI.clickElement(btnNopDonMoi, "Nút [Nộp đơn mới]");
    }
}