#include <stdio.h>

int main() {
	int a = 1;
	int b = 10;
	#pragma acc parallel loop
	for (int i = 0; i < 10; i++) {
		a = b;
	}
	printf("%d\n", a);
}
