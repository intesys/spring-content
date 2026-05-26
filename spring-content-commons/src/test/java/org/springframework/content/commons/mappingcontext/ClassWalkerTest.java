package org.springframework.content.commons.mappingcontext;


import jakarta.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

public class ClassWalkerTest {

    @Test
    @DisplayName("should visit all fields")
    void visitAllFields() {
        ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
        ClassWalker walker = new ClassWalker(visitor);
        walker.accept(TestClass.class);

        ContentProperty expectedProperty = new ContentProperty();
        expectedProperty.setContentPropertyPath("content");
        expectedProperty.setContentIdPropertyPath("contentId");
        expectedProperty.setContentIdType(TypeDescriptor.valueOf(String.class));
        expectedProperty.setContentLengthPropertyPath("contentLength");
        expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
        expectedProperty.setMimeTypePropertyPath("contentMimeType");
        expectedProperty.setOriginalFileNamePropertyPath("contentOriginalFileName");
        assertThat(visitor.getProperties(), hasEntry("content", expectedProperty));

        ContentProperty expectedSubClassProperty = new ContentProperty();
        expectedSubClassProperty.setContentPropertyPath("child.content");
        expectedSubClassProperty.setContentIdPropertyPath("child.contentId");
        expectedSubClassProperty.setContentIdType(TypeDescriptor.valueOf(String.class));
        expectedSubClassProperty.setContentLengthPropertyPath("child.contentLength");
        expectedSubClassProperty.setContentLengthType(TypeDescriptor.valueOf(long.class));
        expectedSubClassProperty.setMimeTypePropertyPath("child.contentMimeType");
        expectedSubClassProperty.setOriginalFileNamePropertyPath("child.contentOriginalFileName");
        assertThat(visitor.getProperties(), hasEntry("child", expectedSubClassProperty));

        ContentProperty expectedSubSubClassProperty = new ContentProperty();
        expectedSubSubClassProperty.setContentPropertyPath("child.subChild.content");
        expectedSubSubClassProperty.setContentIdPropertyPath("child.subChild.contentId");
        expectedSubSubClassProperty.setContentIdType(TypeDescriptor.valueOf(String.class));
        expectedSubSubClassProperty.setContentLengthPropertyPath("child.subChild.contentLength");
        expectedSubSubClassProperty.setContentLengthType(TypeDescriptor.valueOf(int.class));
        expectedSubSubClassProperty.setMimeTypePropertyPath("child.subChild.contentMimeType");
        expectedSubSubClassProperty.setOriginalFileNamePropertyPath("child.subChild.contentOriginalFileName");
        assertThat(visitor.getProperties(), hasEntry("child/subChild", expectedSubSubClassProperty));

        ContentProperty expectedCamelCaseProperty = new ContentProperty();
        expectedCamelCaseProperty.setContentPropertyPath("camelCaseProperty.camelCaseProperty");
        expectedCamelCaseProperty.setContentIdPropertyPath("camelCaseProperty.camelCasePropertyId");
        expectedCamelCaseProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
        expectedCamelCaseProperty.setContentLengthPropertyPath("camelCaseProperty.camelCasePropertyLen");
        expectedCamelCaseProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
        expectedCamelCaseProperty.setMimeTypePropertyPath("camelCaseProperty.camelCasePropertyMimeType");
        expectedCamelCaseProperty.setOriginalFileNamePropertyPath("camelCaseProperty.camelCasePropertyOriginalFileName");
        assertThat(visitor.getProperties(), hasEntry("camelCaseProperty", expectedCamelCaseProperty));

        ContentProperty expectedOtherCamelCaseProperty = new ContentProperty();
        expectedOtherCamelCaseProperty.setContentPropertyPath("otherCamelCaseProperty.camelCaseProperty");
        expectedOtherCamelCaseProperty.setContentIdPropertyPath("otherCamelCaseProperty.camelCasePropertyIds");
        expectedOtherCamelCaseProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
        expectedOtherCamelCaseProperty.setContentLengthPropertyPath("otherCamelCaseProperty.camelCasePropertyLens");
        expectedOtherCamelCaseProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
        expectedOtherCamelCaseProperty.setMimeTypePropertyPath("otherCamelCaseProperty.camelCasePropertyMimetypes");
        expectedOtherCamelCaseProperty.setOriginalFileNamePropertyPath("otherCamelCaseProperty.camelCasePropertyFilenames");
        assertThat(visitor.getProperties(), hasEntry("otherCamelCaseProperty", expectedOtherCamelCaseProperty));
    }

    @Nested
    @DisplayName("given a class with uncorrelated attributes")
    class UncorrelatedAttributes {
        @Test
        @DisplayName("should return two content properties")
        void returnTwoProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(UncorrelatedAttrClass.class);

            ContentProperty expectedProperty2 = new ContentProperty();
            expectedProperty2.setContentPropertyPath("content");
            expectedProperty2.setContentIdPropertyPath("contentId");
            expectedProperty2.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty2.setContentLengthPropertyPath("len");
            expectedProperty2.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty2.setMimeTypePropertyPath("mimeType");
            expectedProperty2.setOriginalFileNamePropertyPath("originalFileName");
            assertThat(visitor.getProperties(), hasEntry("content", expectedProperty2));
        }
    }

    @Nested
    @DisplayName("given a class with correlated attributes")
    class CorrelatedAttributes {
        @Test
        @DisplayName("should return two content properties")
        void returnTwoProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(CorrelatedAttrClass.class);

            ContentProperty expectedProperty2 = new ContentProperty();
            expectedProperty2.setContentPropertyPath("content");
            expectedProperty2.setContentIdPropertyPath("contentId");
            expectedProperty2.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty2.setContentLengthPropertyPath("contentLen");
            expectedProperty2.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty2.setMimeTypePropertyPath("contentMimeType");
            expectedProperty2.setOriginalFileNamePropertyPath("contentOriginalFileName");
            assertThat(visitor.getProperties(), hasEntry("content", expectedProperty2));
        }
    }

    @Nested
    @DisplayName("given a class with a child with uncorrelated attributes")
    class ChildUncorrelatedAttributes {
        @Test
        @DisplayName("should return two content properties")
        void returnTwoProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(TestClass2.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("child.content");
            expectedProperty.setContentIdPropertyPath("child.contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty.setContentLengthPropertyPath("child.len");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty.setMimeTypePropertyPath("child.mimeType");
            expectedProperty.setOriginalFileNamePropertyPath("child.originalFileName");
            assertThat(visitor.getProperties(), hasEntry("child", expectedProperty));
        }
    }

    @Nested
    @DisplayName("given a class with relation attributes")
    class RelationAttributes {
        @Test
        @DisplayName("should not traverse them")
        void noTraverse() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(TestClass3.class);

            assertThat(visitor.getProperties().size(), is(0));
        }
    }

    @Nested
    @DisplayName("given a class with content properties in its super class")
    class SuperClassProperties {
        @Test
        @DisplayName("should visit them")
        void visitSuperClass() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(TestClass4.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("content");
            expectedProperty.setContentIdPropertyPath("contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(String.class));
            expectedProperty.setContentLengthPropertyPath("contentLength");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(int.class));
            expectedProperty.setMimeTypePropertyPath("contentMimeType");
            expectedProperty.setOriginalFileNamePropertyPath("contentOriginalFileName");
            assertThat(visitor.getProperties(), hasEntry("content", expectedProperty));
        }
    }

    @Nested
    @DisplayName("given a class with multiple child content property objects")
    class MultipleChildObjects {
        @Test
        @DisplayName("should return two content properties")
        void returnTwoProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(TestClass5.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("child1.content");
            expectedProperty.setContentIdPropertyPath("child1.contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty.setContentLengthPropertyPath("child1.len");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty.setMimeTypePropertyPath("child1.mimeType");
            expectedProperty.setOriginalFileNamePropertyPath("child1.originalFileName");
            assertThat(visitor.getProperties(), hasEntry("child1", expectedProperty));

            ContentProperty expectedProperty3 = new ContentProperty();
            expectedProperty3.setContentPropertyPath("child2.content");
            expectedProperty3.setContentIdPropertyPath("child2.contentId");
            expectedProperty3.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty3.setContentLengthPropertyPath("child2.len");
            expectedProperty3.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty3.setMimeTypePropertyPath("child2.mimeType");
            expectedProperty3.setOriginalFileNamePropertyPath("child2.originalFileName");
            assertThat(visitor.getProperties(), hasEntry("child2", expectedProperty3));
        }
    }

    @Nested
    @DisplayName("given a class with a child with multiple content property objects")
    class ChildMultipleProperties {
        @Test
        @DisplayName("should return two content properties")
        void returnTwoProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(TestClass6.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("child.content");
            expectedProperty.setContentIdPropertyPath("child.contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty.setContentLengthPropertyPath("child.contentLen");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty.setMimeTypePropertyPath("child.contentMimeType");
            expectedProperty.setOriginalFileNamePropertyPath("child.contentOriginalFileName");
            assertThat(visitor.getProperties(), hasEntry("child/content", expectedProperty));

            ContentProperty expectedProperty3 = new ContentProperty();
            expectedProperty3.setContentPropertyPath("child.preview");
            expectedProperty3.setContentIdPropertyPath("child.previewId");
            expectedProperty3.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty3.setContentLengthPropertyPath("child.previewLen");
            expectedProperty3.setContentLengthType(TypeDescriptor.valueOf(Integer.class));
            expectedProperty3.setMimeTypePropertyPath("child.previewMimeType");
            expectedProperty3.setOriginalFileNamePropertyPath("child.previewOriginalFileName");
            assertThat(visitor.getProperties(), hasEntry("child/preview", expectedProperty3));
        }
    }

    @Nested
    @DisplayName("given a class with a child with a child with multiple content property objects")
    class GrandChildMultipleProperties {
        @Test
        @DisplayName("should return two content properties")
        void returnTwoProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(TestClass7.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("child.child.content");
            expectedProperty.setContentIdPropertyPath("child.child.contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty.setContentLengthPropertyPath("child.child.contentLen");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty.setMimeTypePropertyPath("child.child.contentMimeType");
            expectedProperty.setOriginalFileNamePropertyPath("child.child.contentOriginalFileName");
            assertThat(visitor.getProperties(), hasEntry("child/child/content", expectedProperty));

            ContentProperty expectedProperty2 = new ContentProperty();
            expectedProperty2.setContentPropertyPath("child.child.preview");
            expectedProperty2.setContentIdPropertyPath("child.child.previewId");
            expectedProperty2.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty2.setContentLengthPropertyPath("child.child.previewLen");
            expectedProperty2.setContentLengthType(TypeDescriptor.valueOf(Integer.class));
            expectedProperty2.setMimeTypePropertyPath("child.child.previewMimeType");
            expectedProperty2.setOriginalFileNamePropertyPath("child.child.previewOriginalFileName");
            assertThat(visitor.getProperties(), hasEntry("child/child/preview", expectedProperty2));
        }
    }

    public static class TestClass {

        private TestEnum enums;

        private TestClass circlularRef;

        private String str;
        private int i;
        @ContentId
        private String contentId;
        @ContentLength
        private Long contentLength;
        @MimeType
        private Long contentMimeType;
        @OriginalFileName
        private Long contentOriginalFileName;
        private TestSubClass child;

        private TestCamelCasedProperty camelCaseProperty;
        private OtherTestCamelCasedProperty otherCamelCaseProperty;
    }

    public static class TestSubClass {
        @ContentId
        private String contentId;
        @ContentLength
        private long contentLength;
        @MimeType
        private Long contentMimeType;
        @OriginalFileName
        private Long contentOriginalFileName;
        private TestSubSubClass subChild;
    }

    public static class TestSubSubClass {
        @ContentId
        private String contentId;
        @ContentLength
        private int contentLength;
        @MimeType
        private Long contentMimeType;
        @OriginalFileName
        private Long contentOriginalFileName;
    }

    public static class UncorrelatedAttrClass {
        private @Id @GeneratedValue Long id;
        private String name;
        private @ContentId UUID contentId;
        private @ContentLength Long len;
        private @MimeType String mimeType;
        private @OriginalFileName String originalFileName;
        private String title;
    }

    public static class CorrelatedAttrClass {
        private @Id @GeneratedValue Long id;
        private String name;
        private @ContentId UUID contentId;
        private @ContentLength Long contentLen;
        private @MimeType String contentMimeType;
        private @OriginalFileName String contentOriginalFileName;
        private String title;
    }

    public static class TestClass2 {
        private UncorrelatedAttrClass child;
    }

    public static class TestClass3 {

        @OneToOne
        private TestSubClass oneToOne;
        @OneToMany
        private TestSubClass oneToMany;
        @ManyToOne
        private TestSubClass manyToOne;
        @ManyToMany
        private TestSubClass manyToMany;
        @DBRef
        private TestSubClass docRef;
    }

    public static class TestClass4 extends TestSubSubClass {
    }

    public static class TestClass5 {
        private UncorrelatedAttrClass child1;
        private UncorrelatedAttrClass child2;
    }

    public static class TestClass6 {
        private TestMultipleAttrs child;
    }

    public static class TestMultipleAttrs {
        private @ContentId UUID contentId;
        private @ContentLength Long contentLen;
        private @MimeType String contentMimeType;
        private @OriginalFileName String contentOriginalFileName;

        private @ContentId UUID previewId;
        private @ContentLength Integer previewLen;
        private @MimeType String previewMimeType;
        private @OriginalFileName String previewOriginalFileName;
    }

    public static class TestClass7 {
        private TestClass6 child;
    }

    public static enum TestEnum {
        A, B, C, D
    }

    private static class TestCamelCasedProperty {
        private @ContentId UUID camelCasePropertyId;
        private @ContentLength Long camelCasePropertyLen;
        private @MimeType String camelCasePropertyMimeType;
        private @OriginalFileName String camelCasePropertyOriginalFileName;
    }

    private static class OtherTestCamelCasedProperty {
        private @ContentId UUID camelCasePropertyIds;
        private @ContentLength Long camelCasePropertyLens;
        private @MimeType String camelCasePropertyMimetypes;
        private @OriginalFileName String camelCasePropertyFilenames;
    }
}
