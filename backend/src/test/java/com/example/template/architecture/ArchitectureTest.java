package com.example.template.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
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
    @DisplayName("ARCH-02: Controller는 Entity를 반환하지 않는다")
    void controllersDoNotExposeEntities() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .dependOnClassesThat()
                .areAnnotatedWith(jakarta.persistence.Entity.class)
                .because("Controller는 DTO(record)로만 주고받아야 한다");

        rule.allowEmptyShould(true).check(CLASSES);
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
    @DisplayName("ARCH-04: 도메인은 다른 도메인의 infra에 의존하지 않는다")
    void domainsDoNotDependOnOtherDomainsInfra() {
        ArchRule rule = SlicesRuleDefinition.slices()
                .matching(BASE + ".domain.(*)..")
                .should()
                .notDependOnEachOther()
                .ignoreDependency(
                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                "admin 도메인", clazz -> clazz.getPackageName().contains(".domain.admin")),
                        com.tngtech.archunit.base.DescribedPredicate.alwaysTrue())
                .because("도메인 사이는 application 서비스나 ID로만 참조한다 (admin은 조회 예외)");

        rule.allowEmptyShould(true).check(CLASSES);
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
