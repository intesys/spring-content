package internal.org.springframework.content.commons.store.factory;

import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragments;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.repository.AfterStoreEvent;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.events.*;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.Store;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@SuppressWarnings("unchecked")
public class StoreMethodInterceptorTest {

	private static Method getResourceMethod;
	private static Method getResourceEntityMethod;
	private static Method associateMethod;
	private static Method unassociateMethod;
	private static Method getContentMethod;
	private static Method setContentMethod;
	private static Method setContentFromResourceMethod;
	private static Method unsetContentMethod;
	private static Method toStringMethod;

	static {
		getResourceMethod = ReflectionUtils.findMethod(Store.class, "getResource", Serializable.class);
		getResourceEntityMethod = ReflectionUtils.findMethod(AssociativeStore.class, "getResource", Object.class);
		associateMethod = ReflectionUtils.findMethod(AssociativeStore.class, "associate", Object.class, Serializable.class);
		unassociateMethod = ReflectionUtils.findMethod(AssociativeStore.class, "unassociate", Object.class);
		getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
		setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
		setContentFromResourceMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class);
		unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class);
		toStringMethod = ReflectionUtils.findMethod(Object.class, "toString");
	}

	private StoreMethodInterceptor interceptor;

	// mocks
	private ContentStore<Object, Serializable> store;
	private MethodInvocation invocation;
	private ApplicationEventPublisher publisher;

	private Object result;
	private Exception e;

	private ByteArrayInputStream modifiedStream = null;

	@Nested
	@DisplayName("#invoke")
	class Invoke {

		@BeforeEach
		void setUp() {
			store = mock(ContentStore.class);
			publisher = mock(ApplicationEventPublisher.class);
			e = null;
		}

		private void executeInvoke() {
			interceptor = new StoreMethodInterceptor();
			StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(TestContentStore.class, store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
			interceptor.setStoreFragments(fragments);
			try {
				interceptor.invoke(invocation);
			}
			catch (Throwable invokeException) {
				if (invokeException instanceof Exception) {
					e = (Exception) invokeException;
				} else {
					e = new RuntimeException(invokeException);
				}
			}
		}

		@Nested
		@DisplayName("#getContent")
		class GetContent {
			@BeforeEach
			void setUp() {
				result = new ByteArrayInputStream(new byte[]{});
				when(store.getContent(any(Object.class))).thenReturn((InputStream)result);
				invocation = new TestMethodInvocation(store, getContentMethod, new Object[]{new Object()});
				executeInvoke();
			}

			@Test
			@DisplayName("should proceed")
			void proceed() {
				assertThat(e, is(nullValue()));

				ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
				InOrder inOrder = Mockito.inOrder(publisher, store);

				inOrder.verify(publisher, times(1)).publishEvent(argThat(isA(StoreEvent.class)));
				inOrder.verify(store).getContent(any(Object.class));
				inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
				assertThat(captor.getValue().getResult(), is(result));
			}

			@Nested
			@DisplayName("when getContent is invoked with illegal arguments")
			class IllegalArguments {
				@BeforeEach
				void setUp() {
					invocation = new TestMethodInvocation(store, getContentMethod, new Object[]{});
					executeInvoke();
				}
				@Test
				@DisplayName("should fail")
				void fail() {
					assertThat(e, is(not(nullValue())));
				}
			}
		}

		@Nested
		@DisplayName("#setContent")
		class SetContent {
			@BeforeEach
			void setUp() {
				result = new Object();
				when(store.setContent(any(Object.class), any(InputStream.class))).thenReturn(result);
				invocation = new TestMethodInvocation(store, setContentMethod, new Object[]{new Object(), new ByteArrayInputStream("test".getBytes())});
			}

			@Test
			@DisplayName("should proceed")
			void proceed() throws Exception {
				executeInvoke();
				assertThat(e, is(nullValue()));

				ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
				ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
				ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
				InOrder inOrder = Mockito.inOrder(publisher, store);

				inOrder.verify(publisher, times(1)).publishEvent(beforeArgCaptor.capture());
				assertThat(beforeArgCaptor.getValue().getResource(), is(nullValue()));
				assertThat(beforeArgCaptor.getValue().getInputStream(), is(not(nullValue())));

				inOrder.verify(store).setContent(any(Object.class), setContentArgCaptor.capture());
				try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
					assertThat(IOUtils.toString(setContentInputStream), is("test"));
				}

				inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
				assertThat(afterArgCaptor.getValue().getResult(), is(result));
			}

			@Nested
			@DisplayName("when the BeforeSetContentEvent consumes the entire inputstream")
			class ConsumesEntireStream {
				@BeforeEach
				void setUp() {
					onBeforeSetContentPublishEvent((invocationOnMock) -> {
						try (InputStream is = ((BeforeSetContentEvent)invocationOnMock.getArgument(0)).getInputStream()) {
							assertThat(IOUtils.toString(is), is("test"));
						}
						return null;
					});
					executeInvoke();
				}
				@Test
				@DisplayName("should still receive the inputstream in the setContent invocation")
				void receiveStream() throws Exception {
					assertThat(e, is(nullValue()));

					ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
					ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, store);

					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

					inOrder.verify(store).setContent(any(Object.class), setContentArgCaptor.capture());
					try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
						assertThat(IOUtils.toString(setContentInputStream), is("test"));
					}

					inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
					assertThat(afterArgCaptor.getValue().getResult(), is(result));
				}
			}

			@Nested
			@DisplayName("when the BeforeSetContentEvent consumes partial inputstream")
			class ConsumesPartialStream {
				@BeforeEach
				void setUp() {
					onBeforeSetContentPublishEvent((invocationOnMock) -> {
						InputStream is = ((BeforeSetContentEvent)invocationOnMock.getArgument(0)).getInputStream();
						assertThat((char)is.read(), is('t'));
						assertThat((char)is.read(), is('e'));
						return null;
					});
					executeInvoke();
				}
				@Test
				@DisplayName("should still receive the inputstream in the setContent invocation")
				void receiveStream() throws Exception {
					assertThat(e, is(nullValue()));

					InOrder inOrder = Mockito.inOrder(publisher, store);

					ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
					ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

					inOrder.verify(store).setContent(any(Object.class), setContentArgCaptor.capture());

					try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
						assertThat(IOUtils.toString(setContentInputStream), is("test"));
					}

					inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
					assertThat(afterArgCaptor.getValue().getResult(), is(result));
				}
			}

			@Nested
			@DisplayName("when the BeforeSetContentEvent does not consume any of the inputstream")
			class ConsumesNothing {
				@BeforeEach
				void setUp() {
					onBeforeSetContentPublishEvent((invocationOnMock) -> null);
					executeInvoke();
				}
				@Test
				@DisplayName("should still receive the inputstream in the setContent invocation")
				void receiveStream() throws Exception {
					assertThat(e, is(nullValue()));

					InOrder inOrder = Mockito.inOrder(publisher, store);

					ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
					ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

					inOrder.verify(store).setContent(any(Object.class), setContentArgCaptor.capture());

					try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
						assertThat(IOUtils.toString(setContentInputStream), is("test"));
					}

					inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
					assertThat(afterArgCaptor.getValue().getResult(), is(result));
				}
			}

			@Nested
			@DisplayName("when the BeforeSetContentEvent replaces the inputstream")
			class ReplacesStream {
				@BeforeEach
				void setUp() {
					onBeforeSetContentPublishEvent((invocationOnMock) -> {
						modifiedStream = new ByteArrayInputStream("encrypted".getBytes());
						((BeforeSetContentEvent)invocationOnMock.getArgument(0)).setInputStream(modifiedStream);
						return null;
					});
					executeInvoke();
				}
				@Test
				@DisplayName("should still receive the replaced inputstream in the setContent invocation")
				void receiveReplacedStream() {
					assertThat(e, is(nullValue()));

					InOrder inOrder = Mockito.inOrder(publisher, store);

					ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
					ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

					inOrder.verify(store).setContent(any(Object.class), setContentArgCaptor.capture());

					try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
						assertThat(setContentInputStream, is(modifiedStream));
					} catch (Exception ex) {}

					inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
					assertThat(afterArgCaptor.getValue().getResult(), is(result));
				}
			}

			@Nested
			@DisplayName("when setContent is invoked with illegal arguments")
			class IllegalArguments {
				@BeforeEach
				void setUp() {
					invocation = new TestMethodInvocation(store, setContentMethod, new Object[]{});
					executeInvoke();
				}
				@Test
				@DisplayName("should fail")
				void fail() {
					assertThat(e, is(not(nullValue())));
				}
			}
		}

		@Nested
		@DisplayName("#setContent from Resource")
		class SetContentFromResource {
			@BeforeEach
			void setUp() throws Exception {
				result = new Object();
				when(store.setContent(any(Object.class), any(Resource.class))).thenReturn(result);
				invocation = new TestMethodInvocation(store, setContentFromResourceMethod, new Object[]{new Object(), new InputStreamResource(new ByteArrayInputStream("test".getBytes()))});
				executeInvoke();
			}
			@Test
			@DisplayName("should proceed")
			void proceed() throws Exception {
				assertThat(e, is(nullValue()));

				ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
				ArgumentCaptor<Resource> setContentArgCaptor = ArgumentCaptor.forClass(Resource.class);
				ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
				InOrder inOrder = Mockito.inOrder(publisher, store);

				inOrder.verify(publisher, times(1)).publishEvent(beforeArgCaptor.capture());
				assertThat(beforeArgCaptor.getValue().getResource(), is(not(nullValue())));
				assertThat(beforeArgCaptor.getValue().getInputStream(), is(nullValue()));

				inOrder.verify(store).setContent(any(Object.class), setContentArgCaptor.capture());
				try (InputStream setContentInputStream = setContentArgCaptor.getValue().getInputStream()) {
					assertThat(IOUtils.toString(setContentInputStream), is("test"));
				}

				inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
				assertThat(afterArgCaptor.getValue().getResult(), is(result));
			}
		}

		@Nested
		@DisplayName("#unsetContent")
		class UnsetContent {
			@BeforeEach
			void setUp() {
				result = new Object();
				when(store.unsetContent(any(Object.class))).thenReturn(result);
				invocation = new TestMethodInvocation(store, unsetContentMethod, new Object[]{new Object()});
			}
			@Test
			@DisplayName("should proceed")
			void proceed() {
				executeInvoke();
				assertThat(e, is(nullValue()));

				InOrder inOrder = Mockito.inOrder(publisher, store);

				inOrder.verify(publisher).publishEvent(argThat(isA(BeforeUnsetContentEvent.class)));
				inOrder.verify(store).unsetContent(any(Object.class));

				ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
				inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
				assertThat(captor.getValue().getResult(), is(result));
			}

			@Nested
			@DisplayName("when unsetContent is invoked with illegal arguments")
			class IllegalArguments {
				@BeforeEach
				void setUp() {
					invocation = new TestMethodInvocation(store, unsetContentMethod, new Object[]{});
					executeInvoke();
				}
				@Test
				@DisplayName("should fail")
				void fail() {
					assertThat(e, is(not(nullValue())));
				}
			}
		}

		@Nested
		@DisplayName("#getResource")
		class GetResource {
			@Nested
			@DisplayName("when getResource is invoked")
			class GetResourceSerial {
				@BeforeEach
				void setUp() {
					result = mock(Resource.class);
					when(store.getResource(any(Serializable.class))).thenReturn((Resource)result);
					invocation = new TestMethodInvocation(store, getResourceMethod, new Serializable(){});
					executeInvoke();
				}
				@Test
				@DisplayName("should proceed")
				void proceed() {
					assertThat(e, is(nullValue()));

					ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, store);

					inOrder.verify(publisher, times(1)).publishEvent(argThat(instanceOf(StoreEvent.class)));
					verify(store).getResource(any(Serializable.class));
					inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				}
			}

			@Nested
			@DisplayName("when getResource(entity) is invoked")
			class GetResourceEntity {
				@BeforeEach
				void setUp() {
					result = mock(Resource.class);
					when(store.getResource(argThat(isA(ContentObject.class)))).thenReturn((Resource)result);
					invocation = new TestMethodInvocation(store, getResourceEntityMethod, new Object[]{new ContentObject("text/plain")});
					executeInvoke();
				}
				@Test
				@DisplayName("should proceed")
				void proceed() {
					assertThat(e, is(nullValue()));

					InOrder inOrder = Mockito.inOrder(publisher, store);

					inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeGetResourceEvent.class)));
					inOrder.verify(store).getResource(argThat(isA(ContentObject.class)));

					ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
					inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				}
			}
		}

		@Nested
		@DisplayName("#associate")
		class Associate {
			@BeforeEach
			void setUp() {
				result = mock(Resource.class);
				invocation = new TestMethodInvocation(store, associateMethod, new Object[]{"", 123});
				executeInvoke();
			}
			@Test
			@DisplayName("should proceed")
			void proceed() {
				InOrder inOrder = Mockito.inOrder(publisher, store);
				inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeAssociateEvent.class)));
				inOrder.verify(store).associate(eq(""), eq(123));
				inOrder.verify(publisher).publishEvent(argThat(instanceOf(AfterAssociateEvent.class)));
			}
		}

		@Nested
		@DisplayName("#unassociate")
		class Unassociate {
			@BeforeEach
			void setUp() {
				result = mock(Resource.class);
				invocation = new TestMethodInvocation(store, unassociateMethod, new Object[]{"foo"});
				executeInvoke();
			}
			@Test
			@DisplayName("should proceed")
			void proceed() {
				InOrder inOrder = Mockito.inOrder(publisher, store);
				inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeUnassociateEvent.class)));
				verify(store).unassociate("foo");
				inOrder.verify(publisher).publishEvent(argThat(instanceOf(AfterUnassociateEvent.class)));
			}
		}

		@Nested
		@DisplayName("#toString")
		class ToString {
			@BeforeEach
			void setUp() {
				invocation = new TestMethodInvocation(store, toStringMethod, new Object[]{});
				executeInvoke();
			}
			@Test
			@DisplayName("should proceed")
			void proceed() {
				verify(publisher, never()).publishEvent(any(Object.class));
			}
		}
	}

	@Nested
	@DisplayName("#findMethod")
	class FindMethod {
		@Test
		@DisplayName("should resolve the method when not overridden")
		void resolveNotOverridden() {
			store = mock(ContentStore.class);
			publisher = mock(ApplicationEventPublisher.class);
			interceptor = new StoreMethodInterceptor();
			Method m = ReflectionUtils.findMethod(TestContentStore.class, "unsetContent", Object.class);
			assertThat(m, is(not(nullValue())));
			Method actual = interceptor.getMethod(m, new StoreFragment(TestContentStore.class, new StoreImpl(TestContentStore.class, store, publisher, Paths.get(System.getProperty("java.io.tmpdir")))));
			assertThat(actual, is(ReflectionUtils.findMethod(StoreImpl.class, "unsetContent", Object.class)));
		}

		@Test
		@DisplayName("should resolve the method when it is overridden in the interface")
		void resolveOverridden() {
			store = mock(ContentStore.class);
			publisher = mock(ApplicationEventPublisher.class);
			interceptor = new StoreMethodInterceptor();
			Method m = ReflectionUtils.findMethod(TestContentStore.class, "setContent", TEntity.class, InputStream.class);
			assertThat(m, is(not(nullValue())));
			Method actual = interceptor.getMethod(m, new StoreFragment(TestContentStore.class, new StoreImpl(TestContentStore.class, store, publisher, Paths.get(System.getProperty("java.io.tmpdir")))));
			assertThat(actual, is(ReflectionUtils.findMethod(StoreImpl.class, "setContent", Object.class, InputStream.class)));
		}
	}

	private void onBeforeSetContentPublishEvent(PublishEventAction action) {
		doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				return action.doAction(invocationOnMock);
			}
		}).when(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));
	}

	public interface PublishEventAction {
		Object doAction(InvocationOnMock invocationOnMock) throws Exception;
	}

	public static class TestMethodInvocation extends SimpleMethodInvocation {

		TestMethodInvocation(Object targetObject, Method method, Object... arguments) {
			super(targetObject, method, arguments);
		}

		@Override
		public Object proceed() {
			return ReflectionUtils.invokeMethod(getMethod(), getThis(), getArguments());
		}
	}

	@EqualsAndHashCode
	public static class ContentObject {
		@MimeType
		public String mimeType;

		public ContentObject(String mimeType) {
			this.mimeType = mimeType;
		}
	}

	public interface AContentRepositoryExtension<S> {
		void getCustomContent(S property);
	}

	@Getter
	@Setter
	public class TEntity {

		private UUID contentId;
	}

	public interface TestContentStore extends ContentStore<TEntity, UUID> {
		@Override
		TEntity setContent(TEntity property, InputStream content);
	}
}
