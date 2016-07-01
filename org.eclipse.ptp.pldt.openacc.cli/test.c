#include <stdio.h>

int main() {
	int i, a[100], b[100];
	// refactor
	for (i = 0; i < 100; i++) {
		a[i] = 0;
		b[i] = 100;
	}
	return 0;
}
