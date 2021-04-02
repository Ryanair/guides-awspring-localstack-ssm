package com.ryanair.guide.ssm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SSM;
import static org.testcontainers.utility.DockerImageName.parse;

/**
 * @author Ryanair
 */
@Testcontainers
@SpringBootTest(
        properties = {
                "spring.config.import=aws-parameterstore:/myProject/myService_myProfile",
//                "aws.paramstore.prefix=/myProject",
//                "aws.paramstore.name=myService",
//                "spring.profiles.active=myProfile",
                "cloud.aws.region.use-default-aws-region-chain=true",
                "cloud.aws.stack.auto=false",
                "logging.level.com.amazonaws.util.EC2MetadataUtils=error",
                "logging.level.io.awspring.cloud.paramstore.AwsParamStorePropertySource=debug"
        }
)
class ApplicationTests {

    private static final String FULL_IMAGE_NAME = "localstack/localstack:0.12.6";

    @Container
    public static LocalStackContainer localStack = new LocalStackContainer(parse(FULL_IMAGE_NAME))
            .withServices(SSM);

    @BeforeAll
    static void setupSSM() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "ssm", "put-parameter", "--name", "/myProject/myService_myProfile/myParam", "--type", "String", "--value", "myValue");
    }

    @DynamicPropertySource
    static void dynamicProperties(final DynamicPropertyRegistry registry) {
        registry
                .add("aws.paramstore.endpoint", () -> localStack
                        .getEndpointConfiguration(SSM)
                        .getServiceEndpoint());
    }

    @Autowired
    private Environment environment;

    @Test
    void readFromSSM() {
        //given

        //when
        final String myParam = environment.getProperty("myParam", String.class);

        //then
        assertThat(myParam).isEqualTo("myValue");
    }
}
