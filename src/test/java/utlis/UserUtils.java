package utlis;

import com.github.javafaker.Faker;
import io.qameta.allure.Step;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.file.Paths;

public class UserUtils {

    private final Faker faker = new Faker();

    @Step("generate a random visit purpose")
    public String generateRandomPurpose() {
        return Faker.instance().name().fullName();
    }


    @Step("Generate random visitor first name in English")
    public String generateRandomFirstName() {
        return faker.name().firstName();
    }

    @Step("Generate random visitor last name in English")
    public String generateRandomLastName() {
        return faker.name().lastName();
    }

    @Step("Generate random visitor first name in Arabic")
    public String generateRandomArabicFirstName() {
        String[] arabicFirstNames = {
                "محمد", "أحمد", "محمود", "عمر", "يوسف",
                "علي", "خالد", "مصطفى", "حسن", "إبراهيم"
        };

        return faker.options().option(arabicFirstNames);
    }

    @Step("Generate random visitor last name in Arabic")
    public String generateRandomArabicLastName() {
        String[] arabicLastNames = {
                "محمد", "أحمد", "محمود", "حسن", "علي",
                "عبدالله", "إبراهيم", "السيد", "حسين", "كمال"
        };

        return faker.options().option(arabicLastNames);
    }

    @Step("Generate random Saudi phone number")
    public String generateRandomPhoneNumber() {
        return "55" + faker.number().digits(8);
    }

    @Step("Generate random email")
    public String generateRandomEmail() {
        return "user" + faker.number().digits(6) + "@gmail.com";
    }

    @Step("Generate random company name")
    public String generateRandomCompanyName() {
        return faker.company().name();
    }

    @Step("Generate random Passport/National ID")
    public String generateRandomNationalId() {
        return faker.number().digits(14);
    }

    // Resolved relative to the project root (Maven/IntelliJ's working directory when running
    // tests) rather than hardcoded, so it works on any machine/checkout - not just this one.
    @Step("Get visitor ID image path")
    public String getVisitorImagePath() {
        return Paths.get("src/test/java/images/user.jpg").toAbsolutePath().toString();
    }

//    ABDGHJKLMNPRSTUVXZ
//    remove MP
//    add E

    // Saudi vehicle plate format (confirmed directly): exactly 3 letters + exactly 4 digits,
    // entered as two separate fields.

    @Step("Generate random vehicle plate letters")
    public String generateRandomVehiclePlateLetters() {
        return RandomStringUtils.random(3, "ABDJRSXTGKLZNHUV"); // TODO missing E
    }

    @Step("Generate random vehicle plate digits")
    public String generateRandomVehiclePlateDigits() {
        return faker.numerify("####");
    }
}