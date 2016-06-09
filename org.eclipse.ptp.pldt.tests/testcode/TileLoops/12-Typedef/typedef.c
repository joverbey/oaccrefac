#include <stdint.h>

int main() {

	int array[10][20];

	for (uint32_t i = 0; i < 10; i++) { /*<<<<< 7,1,11,1,3,2,pass*/
		for (int j = 0; j < 20; j++) {
			array[i][j] = 0;
		}
	}

	return 0;
}
