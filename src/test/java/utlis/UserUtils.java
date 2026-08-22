package utlis;

import com.github.javafaker.Faker;
import io.qameta.allure.Step;
import org.apache.commons.lang3.RandomStringUtils;

public class UserUtils {
    @Step("generate a random visit purpose")
    public String generateRandomPurpose(){
        String randomVisitPurpose = Faker.instance().name().fullName();

        return randomVisitPurpose;
    }
}
