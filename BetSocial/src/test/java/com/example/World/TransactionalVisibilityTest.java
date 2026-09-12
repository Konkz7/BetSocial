package com.example.World;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An @Transactional that actually does something.
 *
 * Spring resolves transaction attributes through a proxy, and
 * AnnotationTransactionAttributeSource ignores non-public methods by default.
 * So @Transactional on a package-private method compiles, reads correctly, and
 * silently does nothing - no warning, no error, just no transaction.
 *
 * Both places coins move were written that way: makePrediction took a stake and
 * wrote a ledger entry, and decideApproval paid out an entire table, neither of
 * them atomically. A failure part-way through settlement would have left some
 * winners paid and the bet still PENDING, ready to be approved and paid again.
 *
 * This asks Spring itself rather than checking for the `public` keyword, so it
 * stays honest if the defaults ever change.
 */
@DisplayName("Transactional visibility")
class TransactionalVisibilityTest {

    @Test
    @DisplayName("every @Transactional method actually gets a transaction")
    void everyTransactionalMethodResolves() throws Exception {
        AnnotationTransactionAttributeSource source = new AnnotationTransactionAttributeSource();
        List<String> ignored = new ArrayList<>();

        for (Class<?> type : applicationClasses()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Transactional.class)) {
                    continue;
                }
                if (source.getTransactionAttribute(method, type) == null) {
                    ignored.add(type.getSimpleName() + "." + method.getName()
                            + " (" + visibilityOf(method) + ")");
                }
            }
        }

        assertThat(ignored)
                .as("these carry @Transactional but Spring resolves no transaction for them, "
                        + "so the annotation is doing nothing - make them public")
                .isEmpty();
    }

    private static String visibilityOf(Method method) {
        int m = method.getModifiers();
        if (Modifier.isPublic(m)) return "public";
        if (Modifier.isProtected(m)) return "protected";
        if (Modifier.isPrivate(m)) return "private";
        return "package-private";
    }

    /** Every class in the application, components or not - the annotation can be anywhere. */
    private static List<Class<?>> applicationClasses() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

        Set<BeanDefinition> found = scanner.findCandidateComponents("com.example.World");
        assertThat(found)
                .as("the scan found nothing, so this test would pass without checking anything")
                .isNotEmpty();

        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition definition : found) {
            classes.add(Class.forName(definition.getBeanClassName()));
        }
        return classes;
    }
}
