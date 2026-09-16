package com.example.template.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 아키텍처 규칙 자동 검사 (PRD 8.1 ARCH-01 ~ ARCH-06).
 *
 * <p>사람이 리뷰로 잡던 규칙을 테스트가 대신 잡는다. 규칙을 어기면 CI가 실패한다.
 */
@DisplayName("아키텍처 규칙")
class ArchitectureTest {

    private static final String BASE = "com.example.template";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    @DisplayName("ARCH-01: 계층 의존 방향은 api → application → domain ← infra 이다")
    void layerDependencies() {
        ArchRule rule = Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                // 아직 도메인이 없는 단계(M1)에서도 통과하도록 빈 계층을 허용한다.
                .withOptionalLayers(true)
                .layer("api")
                .definedBy(BASE + ".domain.(*)..api..")
                .layer("application")
                .definedBy(BASE + ".domain.(*)..application..")
                .layer("domain")
                .definedBy(BASE + ".domain.(*)..domain..")
                .layer("infra")
                .definedBy(BASE + ".domain.(*)..infra..")
                .whereLayer("api")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("application")
                .mayOnlyBeAccessedByLayers("api")
                .whereLayer("infra")
                .mayOnlyBeAccessedByLayers("application");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("ARCH-02: Controller는 Entity를 반환하거나 파라미터로 받지 않는다")
    void controllersDoNotExposeEntities() {
        // DTO의 정적 팩터리(UserResponse.from(user))가 Entity를 읽는 것은 허용한다.
        // 금지하는 것은 "HTTP 경계에 Entity가 그대로 드러나는 것"이다.
        ArchRule rule = methods()
                .that()
                .areDeclaredInClassesThat()
                .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .and()
                .arePublic()
                .should(new ArchCondition<JavaMethod>("Entity를 반환하거나 파라미터로 받지 않는다") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        if (isEntity(method.getRawReturnType())) {
                            events.add(SimpleConditionEvent.violated(method, method.getFullName() + " 가 Entity를 반환한다"));
                        }
                        method.getRawParameterTypes().stream()
                                .filter(ArchitectureTest::isEntity)
                                .forEach(parameter -> events.add(SimpleConditionEvent.violated(
                                        method, method.getFullName() + " 가 Entity를 파라미터로 받는다")));
                    }
                })
                .because("Controller는 DTO(record)로만 주고받아야 한다");

        rule.allowEmptyShould(true).check(CLASSES);
    }

    private static boolean isEntity(JavaClass javaClass) {
        return javaClass.isAnnotatedWith(jakarta.persistence.Entity.class);
    }

    @Test
    @DisplayName("ARCH-03: @Transactional은 application 계층에만 붙인다")
    void transactionalOnlyInApplicationLayer() {
        ArchRule rule = methods()
                .that()
                .areAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
                .should()
                .beDeclaredInClassesThat()
                .resideInAnyPackage("..application..", "..global..")
                .because("트랜잭션 경계는 application 계층이 정한다");

        rule.allowEmptyShould(true).check(CLASSES);
    }

    @Test
    @DisplayName("ARCH-04: 도메인은 다른 도메인의 infra나 Repository에 직접 의존하지 않는다")
    void domainsDoNotDependOnOtherDomainsInternals() {
        // 허용: auth.application → user.application (application 서비스를 통한 참조)
        // 금지: auth.application → user.infra.jpa (다른 도메인의 저장소에 직접 접근)
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage(BASE + ".domain..")
                .should(new ArchCondition<JavaClass>("다른 도메인의 infra 패키지에 의존한다") {
                    @Override
                    public void check(JavaClass item, ConditionEvents events) {
                        String myDomain = domainNameOf(item.getPackageName());
                        item.getDirectDependenciesFromSelf().stream()
                                .map(dependency -> dependency.getTargetClass().getPackageName())
                                .filter(target -> target.startsWith(BASE + ".domain."))
                                .filter(target -> target.contains(".infra"))
                                .filter(target -> !domainNameOf(target).equals(myDomain))
                                .forEach(target -> events.add(
                                        SimpleConditionEvent.violated(item, item.getName() + " → " + target)));
                    }
                })
                .because("도메인 사이는 application 서비스나 ID로만 참조한다");

        rule.allowEmptyShould(true).check(CLASSES);
    }

    /** com.example.template.domain.user.infra.jpa → "user" */
    private static String domainNameOf(String packageName) {
        String prefix = BASE + ".domain.";
        if (!packageName.startsWith(prefix)) {
            return "";
        }
        String rest = packageName.substring(prefix.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    @Test
    @DisplayName("ARCH-05: 패키지 사이에 순환 의존이 없다")
    void noCycles() {
        ArchRule rule =
                SlicesRuleDefinition.slices().matching(BASE + ".(*)..").should().beFreeOfCycles();

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("ARCH-06: 필드 주입(@Autowired 필드)을 쓰지 않는다")
    void noFieldInjection() {
        ArchRule rule = fields().should()
                .notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                .because("의존성은 생성자로 주입한다");

        rule.allowEmptyShould(true).check(CLASSES);
    }
}
