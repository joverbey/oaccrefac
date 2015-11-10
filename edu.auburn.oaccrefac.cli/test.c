
/* Unroll test. */

int main(int argc, char **argv)
{
	int n[10];
	/* Autotune */
	int i = 0;
	for (; i < 10 - 2; i++) {
		n[i] = i;
		i++;
		n[i] = i;
		i++;
		n[i] = i;
		i++;
		n[i] = i;
	}
	n[i] = i;
	i++;
	n[i] = i;
	i++;

}

