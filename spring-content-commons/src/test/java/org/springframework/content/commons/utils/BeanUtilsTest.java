package org.springframework.content.commons.utils;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class BeanUtilsTest {

	private TestEntity testEntity;

	@Nested
	@DisplayName("setFieldWithAnnotation")
	class SetFieldWithAnnotation {

		@Nested
		@DisplayName("given a simple, non-inheriting class")
		class GivenSimpleClass {

			@BeforeEach
			void setUp() {
				testEntity = new TestEntity();
			}

			@Test
			@DisplayName("should set field directly")
			void shouldSetFieldDirectly() {
				BeanUtils.setFieldWithAnnotation(testEntity, ContentId.class, "a value");
				assertThat(testEntity.fieldOnly, is("a value"));
			}

			@Test
			@DisplayName("should set field via its setter")
			void shouldSetFieldViaItsSetter() {
				BeanUtils.setFieldWithAnnotation(testEntity, ContentLength.class, "b value");
				assertThat(testEntity.getFieldWithGetterSetter(), is("b value"));
			}

			@Test
			@DisplayName("should not fail when told to set on a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.setFieldWithAnnotation(testEntity, Override.class, "value");
				});
			}
		}

		@Nested
		@DisplayName("given an inheriting class")
		class GivenInheritingClass {

			@BeforeEach
			void setUp() {
				testEntity = new InheritingTestEntity();
			}

			@Test
			@DisplayName("should set field directly")
			void shouldSetFieldDirectly() {
				BeanUtils.setFieldWithAnnotation(testEntity, ContentId.class, "a value");
				assertThat(testEntity.fieldOnly, is("a value"));
			}

			@Test
			@DisplayName("should set field via its setter")
			void shouldSetFieldViaItsSetter() {
				BeanUtils.setFieldWithAnnotation(testEntity, ContentLength.class, "b value");
				assertThat(testEntity.getFieldWithGetterSetter(), is("b value"));
			}

			@Test
			@DisplayName("should not fail when told to set on a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.setFieldWithAnnotation(testEntity, Override.class, "value");
				});
			}
		}
	}

	@Nested
	@DisplayName("setFieldWithAnnotationConditionally")
	class SetFieldWithAnnotationConditionally {

		@Nested
		@DisplayName("given a simple, non-inheriting class")
		class GivenSimpleClass {

			@BeforeEach
			void setUp() {
				testEntity = new TestEntity();
			}

			@Test
			@DisplayName("should set field if the condition matches")
			void shouldSetFieldIfConditionMatches() {
				BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentId.class, "a value", new MatchingCondition());
				assertThat(testEntity.fieldOnly, is("a value"));
			}

			@Test
			@DisplayName("should set field via its setter if the condition matches")
			void shouldSetFieldViaSetterIfConditionMatches() {
				BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentLength.class, "b value", new MatchingCondition());
				assertThat(testEntity.getFieldWithGetterSetter(), is("b value"));
			}

			@Test
			@DisplayName("should not set field if the condition does not match")
			void shouldNotSetFieldIfConditionDoesNotMatch() {
				BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentId.class, "a value", new UnmatchingCondition());
				assertThat(testEntity.fieldOnly, is(nullValue()));
			}

			@Test
			@DisplayName("should not set field via its setter if the condition does not match")
			void shouldNotSetFieldViaSetterIfConditionDoesNotMatch() {
				BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentLength.class, "b value", new UnmatchingCondition());
				assertThat(testEntity.getFieldWithGetterSetter(), is(nullValue()));
			}

			@Test
			@DisplayName("should not fail when told to set on a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.setFieldWithAnnotation(testEntity, Override.class, "value");
				});
			}
		}

		@Nested
		@DisplayName("given an inheriting class")
		class GivenInheritingClass {

			@BeforeEach
			void setUp() {
				testEntity = new InheritingTestEntity();
			}

			@Test
			@DisplayName("should set field if the condition matches")
			void shouldSetFieldIfConditionMatches() {
				BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentId.class, "a value", new MatchingCondition());
				assertThat(testEntity.fieldOnly, is("a value"));
			}

			@Test
			@DisplayName("should set field via its setter if the condition matches")
			void shouldSetFieldViaSetterIfConditionMatches() {
				BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentLength.class, "b value", new MatchingCondition());
				assertThat(testEntity.getFieldWithGetterSetter(), is("b value"));
			}

			@Test
			@DisplayName("should not set field if the condition does not match")
			void shouldNotSetFieldIfConditionDoesNotMatch() {
				BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentId.class, "a value", new UnmatchingCondition());
				assertThat(testEntity.fieldOnly, is(nullValue()));
			}

			@Test
			@DisplayName("should not set field via its setter if the condition does not match")
			void shouldNotSetFieldViaSetterIfConditionDoesNotMatch() {
				BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentLength.class, "b value", new UnmatchingCondition());
				assertThat(testEntity.getFieldWithGetterSetter(), is(nullValue()));
			}

			@Test
			@DisplayName("should not fail when told to set on a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.setFieldWithAnnotation(testEntity, Override.class, "value");
				});
			}
		}
	}

	@Nested
	@DisplayName("getFieldWithAnnotation")
	class GetFieldWithAnnotation {

		@Nested
		@DisplayName("given a simple, non-inheriting class")
		class GivenSimpleClass {

			@BeforeEach
			void setUp() {
				testEntity = new TestEntity();
				testEntity.fieldOnly = "a value";
				testEntity.setFieldWithGetterSetter("b value");
			}

			@Test
			@DisplayName("should get field directly")
			void shouldGetFieldDirectly() {
				Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentId.class);
				assertThat(value, is("a value"));
			}

			@Test
			@DisplayName("should get field via its getter")
			void shouldGetFieldViaItsGetter() {
				Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentLength.class);
				assertThat(value, is("b value"));
			}

			@Test
			@DisplayName("should not fail when told to get on a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.getFieldWithAnnotation(testEntity, Override.class);
				});
			}
		}

		@Nested
		@DisplayName("given an inheriting class")
		class GivenInheritingClass {

			@BeforeEach
			void setUp() {
				testEntity = new InheritingTestEntity();
				testEntity.fieldOnly = "a value";
				testEntity.setFieldWithGetterSetter("b value");
			}

			@Test
			@DisplayName("should get field directly")
			void shouldGetFieldDirectly() {
				Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentId.class);
				assertThat(value, is("a value"));
			}

			@Test
			@DisplayName("should get field via its getter")
			void shouldGetFieldViaItsGetter() {
				Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentLength.class);
				assertThat(value, is("b value"));
			}

			@Test
			@DisplayName("should not fail when told to get on a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.getFieldWithAnnotation(testEntity, Override.class);
				});
			}
		}
	}

	@Nested
	@DisplayName("hasFieldWithAnnotation")
	class HasFieldWithAnnotation {

		@Nested
		@DisplayName("given a simple, non-inheriting class")
		class GivenSimpleClass {

			@BeforeEach
			void setUp() {
				testEntity = new TestEntity();
			}

			@Test
			@DisplayName("should return true for annotated public fields")
			void shouldReturnTrueForAnnotatedPublicFields() {
				assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentId.class), is(true));
			}

			@Test
			@DisplayName("should return true for annotated private fields with getter")
			void shouldReturnTrueForAnnotatedPrivateFieldsWithGetter() {
				assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentLength.class), is(true));
			}

			@Test
			@DisplayName("should not fail when about a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.hasFieldWithAnnotation(testEntity, Override.class);
				});
			}
		}

		@Nested
		@DisplayName("given an inheriting class")
		class GivenInheritingClass {

			@BeforeEach
			void setUp() {
				testEntity = new InheritingTestEntity();
			}

			@Test
			@DisplayName("should return true for annotated public fields")
			void shouldReturnTrueForAnnotatedPublicFields() {
				assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentId.class), is(true));
			}

			@Test
			@DisplayName("should return true for annotated private fields with getter")
			void shouldReturnTrueForAnnotatedPrivateFieldsWithGetter() {
				assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentLength.class), is(true));
			}

			@Test
			@DisplayName("should not fail when about a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.hasFieldWithAnnotation(testEntity, Override.class);
				});
			}
		}
	}

	@Nested
	@DisplayName("getFieldWithAnnotationType")
	class GetFieldWithAnnotationType {

		@Nested
		@DisplayName("given a simple, non-inheriting class")
		class GivenSimpleClass {

			@BeforeEach
			void setUp() {
				testEntity = new TestEntity();
			}

			@Test
			@DisplayName("should return true for annotated public fields")
			void shouldReturnTrueForAnnotatedPublicFields() {
				assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentId.class), is(equalTo(String.class)));
			}

			@Test
			@DisplayName("should return true for annotated private fields with getter")
			void shouldReturnTrueForAnnotatedPrivateFieldsWithGetter() {
				assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentLength.class), is(equalTo(String.class)));
			}

			@Test
			@DisplayName("should not fail when asked about a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.getFieldWithAnnotationType(testEntity, Override.class);
				});
			}
		}

		@Nested
		@DisplayName("given an inheriting class")
		class GivenInheritingClass {

			@BeforeEach
			void setUp() {
				testEntity = new InheritingTestEntity();
			}

			@Test
			@DisplayName("should return true for annotated public fields")
			void shouldReturnTrueForAnnotatedPublicFields() {
				assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentId.class), is(equalTo(String.class)));
			}

			@Test
			@DisplayName("should return true for annotated private fields with getter")
			void shouldReturnTrueForAnnotatedPrivateFieldsWithGetter() {
				assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentLength.class), is(equalTo(String.class)));
			}

			@Test
			@DisplayName("should not fail when asked about a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.getFieldWithAnnotationType(testEntity, Override.class);
				});
			}
		}
	}

	@Nested
	@DisplayName("findFieldWithAnnotation")
	class FindFieldWithAnnotation {

		@Nested
		@DisplayName("given a simple, non-inheriting class")
		class GivenSimpleClass {

			@BeforeEach
			void setUp() {
				testEntity = new TestEntity();
			}

			@Test
			@DisplayName("should find fields")
			void shouldFindFields() {
				assertThat(BeanUtils.findFieldWithAnnotation(testEntity, ContentId.class), is(not(nullValue())));
			}

			@Test
			@DisplayName("should find fields with getters")
			void shouldFindFieldsWithGetters() {
				assertThat(BeanUtils.findFieldWithAnnotation(testEntity, ContentLength.class), is(not(nullValue())));
			}

			@Test
			@DisplayName("should not fail when about a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.findFieldWithAnnotation(testEntity, Override.class);
				});
			}
		}

		@Nested
		@DisplayName("given an inheriting class")
		class GivenInheritingClass {

			@BeforeEach
			void setUp() {
				testEntity = new InheritingTestEntity();
			}

			@Test
			@DisplayName("should find fields")
			void shouldFindFields() {
				assertThat(BeanUtils.findFieldWithAnnotation(testEntity, ContentId.class), is(not(nullValue())));
			}

			@Test
			@DisplayName("should find fields with getters")
			void shouldFindFieldsWithGetters() {
				assertThat(BeanUtils.findFieldWithAnnotation(testEntity, ContentLength.class), is(not(nullValue())));
			}

			@Test
			@DisplayName("should not fail when about a missing field")
			void shouldNotFailOnMissingField() {
				assertDoesNotThrow(() -> {
					BeanUtils.findFieldWithAnnotation(testEntity, Override.class);
				});
			}
		}
	}

	@Nested
	@DisplayName("findFieldsWithAnnotation")
	class FindFieldsWithAnnotation {

		@Test
		@DisplayName("should find fields")
		void shouldFindFields() {
			assertThat(BeanUtils.findFieldsWithAnnotation(TestEntity2.class, MimeType.class, new BeanWrapperImpl(new TestEntity2())).length, is(1));
		}
	}

	@Nested
	@DisplayName("getFieldsWithAnnotation")
	class GetFieldsWithAnnotation {

		@Test
		@DisplayName("should find fields")
		void shouldFindFields() {
			TestEntity2 t = new TestEntity2();
			t.setContentId("100");
			t.setOtherContentId("200");

			assertThat(BeanUtils.getFieldsWithAnnotation(t, ContentId.class), is(new Object[]{"100", "200"}));
		}
	}

	@Nested
	@DisplayName("setFieldWithAnnotation (multiple matches)")
	class SetFieldWithAnnotationMultipleMatches {

		@Test
		@DisplayName("should set the field matching the additional condition")
		void shouldSetFieldMatchingCondition() {
			TestEntity2 entity = new TestEntity2();
			BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, "a value", new Condition() {
				@Override
				public boolean matches(Field field) {
					return field.getName().equals("otherContentId");
				}
			});
			assertThat(entity.getContentId(), is(nullValue()));
			assertThat(entity.getOtherContentId(), is("a value"));
		}
	}

	public static class TestEntity {
		@ContentId
		public String fieldOnly;
		@ContentLength
		private String fieldWithGetterSetter;

		public TestEntity() {
		}

		public String getFieldWithGetterSetter() {
			return fieldWithGetterSetter;
		}

		public void setFieldWithGetterSetter(String fieldWithGetterSetter) {
			this.fieldWithGetterSetter = fieldWithGetterSetter;
		}
	}

	public static class InheritingTestEntity extends TestEntity {
	}

	public static class TestEntity2 {
		@ContentId
		public String contentId;
		@ContentLength
		private String contentLen;
		@MimeType
		private String mimeType;

		@ContentId
		public String otherContentId;
		@ContentLength
		private String otherContentLen;

		public String getContentId() {
			return contentId;
		}

		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		public String getContentLen() {
			return contentLen;
		}

		public void setContentLen(String contentLen) {
			this.contentLen = contentLen;
		}

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public String getOtherContentId() {
			return otherContentId;
		}

		public void setOtherContentId(String otherContentId) {
			this.otherContentId = otherContentId;
		}

		public String getOtherContentLen() {
			return otherContentLen;
		}

		public void setOtherContentLen(String otherContentLen) {
			this.otherContentLen = otherContentLen;
		}
	}

	public static class MatchingCondition implements Condition {
		@Override
		public boolean matches(Field field) {
			return true;
		}
	}

	public static class UnmatchingCondition implements Condition {
		@Override
		public boolean matches(Field field) {
			return false;
		}
	}
}
