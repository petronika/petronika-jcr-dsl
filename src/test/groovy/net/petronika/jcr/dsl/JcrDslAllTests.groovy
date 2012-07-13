package net.petronika.jcr.dsl

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite.class)
@Suite.SuiteClasses([
	JcrBuilderTest.class,
	JcrSchemaBuilderTest.class
])
class JcrDslAllTests {
}