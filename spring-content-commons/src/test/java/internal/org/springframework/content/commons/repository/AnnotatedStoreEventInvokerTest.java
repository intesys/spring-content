package internal.org.springframework.content.commons.repository;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.content.commons.annotations.HandleAfterAssociate;
import org.springframework.content.commons.annotations.HandleAfterGetContent;
import org.springframework.content.commons.annotations.HandleAfterGetResource;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleAfterUnassociate;
import org.springframework.content.commons.annotations.HandleAfterUnsetContent;
import org.springframework.content.commons.annotations.HandleBeforeAssociate;
import org.springframework.content.commons.annotations.HandleBeforeGetContent;
import org.springframework.content.commons.annotations.HandleBeforeGetResource;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnassociate;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.events.AfterAssociateEvent;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterGetResourceEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnassociateEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeAssociateEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetResourceEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnassociateEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@SuppressWarnings("unchecked")
public class AnnotatedStoreEventInvokerTest {

	private AnnotatedStoreEventInvoker invoker;

	private StoreEvent event;

	// mocks
	private ReflectionService reflectionService;
	private ContentStore<Object, Serializable> store;

	// event handlers
	private HighestPriorityCustomEventHandler priorityHandler = new HighestPriorityCustomEventHandler();

	@Nested
	@DisplayName("#postProcessAfterInitialization")
	class PostProcessAfterInitialization {
		@BeforeEach
		void setUp() {
			store = mock(ContentStore.class);
			reflectionService = mock(ReflectionService.class);
			invoker = new AnnotatedStoreEventInvoker(reflectionService);
			invoker.postProcessAfterInitialization(new CustomEventHandler(), "custom-bean");
		}

		@Test
		@DisplayName("register the handlers")
		void registerHandlers() {
			assertThat(invoker.getHandlers().get(BeforeGetResourceEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(AfterGetResourceEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(BeforeAssociateEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(AfterAssociateEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(BeforeUnassociateEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(AfterUnassociateEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(BeforeGetContentEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(AfterGetContentEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(BeforeSetContentEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(AfterSetContentEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(BeforeUnsetContentEvent.class).size(), is(2));
			assertThat(invoker.getHandlers().get(AfterUnsetContentEvent.class) .size(), is(2));
		}

		@Nested
		@DisplayName("when initialized with another event handler of highest priority")
		class HighestPriority {
			@BeforeEach
			void setUp() {
				invoker.postProcessAfterInitialization(priorityHandler, "high-priority-custom-bean");
			}
			@Test
			@DisplayName("should order the handlers by priority")
			void shouldOrderHandlers() {
				assertThat(invoker.getHandlers().get(BeforeGetResourceEvent.class).size(), is(3));
				assertThat(invoker.getHandlers().get(BeforeGetResourceEvent.class).get(0).handler, is(priorityHandler));
			}
		}
	}

	@Nested
	@DisplayName("#onApplicationEvent")
	class OnApplicationEvent {
		@BeforeEach
		void setUp() {
			reflectionService = mock(ReflectionService.class);
			invoker = new AnnotatedStoreEventInvoker(reflectionService);
			invoker.postProcessAfterInitialization(new CustomEventHandler(), "custom-bean");
		}

		private void fireEvent() {
			invoker.onApplicationEvent(event);
		}

		@Test
		@DisplayName("given an event handler and a BeforeGetResource event")
		void beforeGetResource() {
			EventSource source = new EventSource();
			event = new BeforeGetResourceEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeGetResource", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a BeforeGetResource event")
		void beforeGetResourceWithEvent() {
			EventSource source = new EventSource();
			event = new BeforeGetResourceEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeGetResource", BeforeGetResourceEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a AfterGetResource event")
		void afterGetResource() {
			EventSource source = new EventSource();
			event = new AfterGetResourceEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterGetResource", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a AfterGetResource event")
		void afterGetResourceWithEvent() {
			EventSource source = new EventSource();
			event = new AfterGetResourceEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterGetResource", AfterGetResourceEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a BeforeAssociate event")
		void beforeAssociate() {
			EventSource source = new EventSource();
			event = new BeforeAssociateEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeAssociate", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a BeforeAssociate event")
		void beforeAssociateWithEvent() {
			EventSource source = new EventSource();
			event = new BeforeAssociateEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeAssociate", BeforeAssociateEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a AfterAssociate event")
		void afterAssociate() {
			EventSource source = new EventSource();
			event = new AfterAssociateEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterAssociate", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a AfterAssociate event")
		void afterAssociateWithEvent() {
			EventSource source = new EventSource();
			event = new AfterAssociateEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterAssociate", AfterAssociateEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a BeforeUnassociate event")
		void beforeUnassociate() {
			EventSource source = new EventSource();
			event = new BeforeUnassociateEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeUnassociate", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a BeforeUnassociate event")
		void beforeUnassociateWithEvent() {
			EventSource source = new EventSource();
			event = new BeforeUnassociateEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeUnassociate", BeforeUnassociateEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a AfterUnassociate event")
		void afterUnassociate() {
			EventSource source = new EventSource();
			event = new AfterUnassociateEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterUnassociate", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a AfterUnassociate event")
		void afterUnassociateWithEvent() {
			EventSource source = new EventSource();
			event = new AfterUnassociateEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterUnassociate", AfterUnassociateEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a BeforeGetContent event")
		void beforeGetContent() {
			EventSource source = new EventSource();
			event = new BeforeGetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeGetContent", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a BeforeGetContent event")
		void beforeGetContentWithEvent() {
			EventSource source = new EventSource();
			event = new BeforeGetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeGetContent", BeforeGetContentEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a AfterGetContent event")
		void afterGetContent() {
			EventSource source = new EventSource();
			event = new AfterGetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterGetContent", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a AfterGetContent event")
		void afterGetContentWithEvent() {
			EventSource source = new EventSource();
			event = new AfterGetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterGetContent", AfterGetContentEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a BeforeSetContent event")
		void beforeSetContent() {
			EventSource source = new EventSource();
			event = new BeforeSetContentEvent(source, store, (InputStream)null);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeSetContent", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a BeforeSetContent event")
		void beforeSetContentWithEvent() {
			EventSource source = new EventSource();
			event = new BeforeSetContentEvent(source, store, (InputStream)null);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeSetContent", BeforeSetContentEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a AfterSetContent event")
		void afterSetContent() {
			EventSource source = new EventSource();
			event = new AfterSetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterSetContent", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a AfterSetContent event")
		void afterSetContentWithEvent() {
			EventSource source = new EventSource();
			event = new AfterSetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterSetContent", AfterSetContentEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a BeforeUnsetContent event")
		void beforeUnsetContent() {
			EventSource source = new EventSource();
			event = new BeforeUnsetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeUnsetContent", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a BeforeUnsetContent event")
		void beforeUnsetContentWithEvent() {
			EventSource source = new EventSource();
			event = new BeforeUnsetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeUnsetContent", BeforeUnsetContentEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and a AfterUnsetContent event")
		void afterUnsetContent() {
			EventSource source = new EventSource();
			event = new AfterUnsetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterUnsetContent", Object.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
		}

		@Test
		@DisplayName("given an event handler accepting the event and a AfterUnsetContent event")
		void afterUnsetContentWithEvent() {
			EventSource source = new EventSource();
			event = new AfterUnsetContentEvent(source, store);
			fireEvent();

			Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterUnsetContent", AfterUnsetContentEvent.class);
			verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event)));
		}

		@Test
		@DisplayName("given an event handler and an unknown event")
		void unknownEvent() {
			EventSource source = new EventSource();
			event = new UnknownContentEvent(source, store);
			fireEvent();

			verify(reflectionService, never()).invokeMethod(any(Method.class), any(Object.class), any(Object.class));
		}
	}

	@StoreEventHandler
	public class CustomEventHandler {

		@HandleBeforeGetResource
		public void beforeGetResource(Object contentObject) {
		}

		@HandleBeforeGetResource
		public void beforeGetResource(BeforeGetResourceEvent event) {
		}

		@HandleAfterGetResource
		public void afterGetResource(Object contentObject) {
		}

		@HandleAfterGetResource
		public void afterGetResource(AfterGetResourceEvent event) {
		}

		@HandleBeforeAssociate
		public void beforeAssociate(Object contentObject) {
		}

		@HandleBeforeAssociate
		public void beforeAssociate(BeforeAssociateEvent event) {
		}

		@HandleAfterAssociate
		public void afterAssociate(Object contentObject) {
		}

		@HandleAfterAssociate
		public void afterAssociate(AfterAssociateEvent event) {
		}

		@HandleBeforeUnassociate
		public void beforeUnassociate(Object contentObject) {
		}

		@HandleBeforeUnassociate
		public void beforeUnassociate(BeforeUnassociateEvent event) {
		}

		@HandleAfterUnassociate
		public void afterUnassociate(Object contentObject) {
		}

		@HandleAfterUnassociate
		public void afterUnassociate(AfterUnassociateEvent event) {
		}

		@HandleBeforeGetContent
		public void beforeGetContent(Object contentObject) {
		}

		@HandleBeforeGetContent
		public void beforeGetContent(BeforeGetContentEvent event) {
		}

		@HandleAfterGetContent
		public void afterGetContent(Object contentObject) {
		}

		@HandleAfterGetContent
		public void afterGetContent(AfterGetContentEvent event) {
		}

		@HandleBeforeSetContent
		public void beforeSetContent(Object contentObject) {
		}

		@HandleBeforeSetContent
		public void beforeSetContent(BeforeSetContentEvent event) {
		}

		@HandleAfterSetContent
		public void afterSetContent(Object contentObject) {
		}

		@HandleAfterSetContent
		public void afterSetContent(AfterSetContentEvent event) {
		}

		@HandleBeforeUnsetContent
		public void beforeUnsetContent(Object contentObject) {
		}

		@HandleBeforeUnsetContent
		public void beforeUnsetContent(BeforeUnsetContentEvent event) {
		}

		@HandleAfterUnsetContent
		public void afterUnsetContent(Object contentObject) {
		}

		@HandleAfterUnsetContent
		public void afterUnsetContent(AfterUnsetContentEvent event) {
		}
	}

	@StoreEventHandler
	public class HighestPriorityCustomEventHandler {

		@HandleBeforeGetResource
		@Order(Ordered.HIGHEST_PRECEDENCE)
		public void beforeGetResource(Object contentObject) {
		}
	}

	public class EventSource {
	}

	public class UnknownContentEvent extends StoreEvent {

		private static final long serialVersionUID = 4393640168031790561L;

		public UnknownContentEvent(Object source,
				ContentStore<Object, Serializable> store) {
			super(source, store);
		}
	}
}
