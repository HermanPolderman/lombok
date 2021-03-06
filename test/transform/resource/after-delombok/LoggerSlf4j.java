class LoggerSlf4j {
	@java.lang.SuppressWarnings("all")
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggerSlf4j.class);
}
class LoggerSlf4jWithImport {
	@java.lang.SuppressWarnings("all")
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggerSlf4jWithImport.class);
}
class LoggerSlf4jOuter {
	static class Inner {
		@java.lang.SuppressWarnings("all")
		private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Inner.class);
	}
}

class LoggerSlf4jWithDifferentLoggerName {
	@java.lang.SuppressWarnings("all")
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("DifferentLogger");
}