package com.justincranford.oteldemo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ContextIT extends AbstractIT {
	@Test
	void testContexts() {
		assertThat(super.applicationContext()).isNotNull();
	}
}
