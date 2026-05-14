package internal.org.springframework.versions.jpa;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@DisplayName("JpaVersioningServiceImpl")
public class JpaVersioningServiceImplTest {

    private VersioningService versioner;

    private Object result;

    private EntityManager em;

    private TestEntity entity, successor, ancestralRoot, ancestor;
    private String versionNo, versionLabel;

    @BeforeEach
    void init() {
        versioner = new JpaVersioningServiceImpl(em);
    }

    @Nested
    @DisplayName("#establishAncestralRoot")
    class EstablishAncestralRoot {

        @BeforeEach
        void setupAndCall() {
            entity = new TestEntity();
            result = versioner.establishAncestralRoot(entity);
        }

        @Test
        @DisplayName("should set the @AncestorId to null")
        void shouldSetAncestorIdToNull() {
            assertThat(entity.getAncestorId(), is(nullValue()));
        }

        @Test
        @DisplayName("should set the @AncestorRootId to it's own id")
        void shouldSetAncestorRootId() {
            assertThat(entity.getAncestorRootId(), is(entity.getId()));
        }

        @Test
        @DisplayName("should return the entity")
        void shouldReturnEntity() {
            assertThat(result, is(entity));
        }
    }

    @Nested
    @DisplayName("#establishAncestor")
    class EstablishAncestor {

        @BeforeEach
        void setupAndCall() {
            entity = new TestEntity();
            successor = new TestEntity();
            successor.setId(999L);
            result = versioner.establishAncestor(entity, successor);
        }

        @Test
        @DisplayName("should set the @SuccessorId to null")
        void shouldSetSuccessorId() {
            assertThat(entity.getSuccessorId(), is(999L));
        }

        @Test
        @DisplayName("should return the entity")
        void shouldReturnEntity() {
            assertThat(result, is(entity));
        }
    }

    @Nested
    @DisplayName("#establishSuccessor")
    class EstablishSuccessor {

        @BeforeEach
        void setupAndCall() {
            successor = new TestEntity();
            ancestralRoot = new TestEntity();
            ancestralRoot.setId(1234L);
            ancestor = new TestEntity();
            ancestor.setId(5678L);
            versionNo = "1.1";
            versionLabel = "a new version";
            result = versioner.establishSuccessor(successor, versionNo, versionLabel, ancestralRoot, ancestor);
        }

        @Test
        @DisplayName("should set the @VersionNumber")
        void shouldSetVersionNumber() {
            assertThat(successor.getVersionNo(), is("1.1"));
        }

        @Test
        @DisplayName("should set the @VersionLabel")
        void shouldSetVersionLabel() {
            assertThat(successor.getVersionLabel(), is("a new version"));
        }

        @Test
        @DisplayName("should set the @SuccessorId to null")
        void shouldSetSuccessorIdToNull() {
            assertThat(successor.getSuccessorId(), is(nullValue()));
        }

        @Test
        @DisplayName("should set the @AncestorRootId")
        void shouldSetAncestorRootId() {
            assertThat(successor.getAncestorRootId(), is(1234L));
        }

        @Test
        @DisplayName("should set the @AncestorId")
        void shouldSetAncestorId() {
            assertThat(successor.getAncestorId(), is(5678L));
        }

        @Test
        @DisplayName("should return the entity")
        void shouldReturnEntity() {
            assertThat(result, is(successor));
        }
    }

    @Getter
    @Setter
    private class TestEntity {
        @Id
        private Long id;
        @Version
        private Long version;
        @AncestorId
        private Long ancestorId;
        @AncestorRootId
        private Long ancestorRootId;
        @SuccessorId
        private Long successorId;
        @VersionNumber
        private String versionNo;
        @VersionLabel
        private String versionLabel;
    }
}
