#include <stdio.h>

int main() {
	int a = 1;
	#pragma acc parallel loop
	for (int i = 0; i < 10; i++) {
		a = i + 1;
	}
	printf("%d\n", a);
}
