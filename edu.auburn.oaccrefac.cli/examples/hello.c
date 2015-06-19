/* Hello, world! */

#include <stdio.h>
#include <stdlib.h>

int main(int argc, char **argv)
{
	char msg[] = "GdkknVnqkc";
	int n = 10;
	for (int i = 0; i < n; i++)
	{
		msg[i] = msg[i] + 1;
	}

	println(msg);
	return EXIT_SUCCESS;
}
