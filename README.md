# oaccrefac

### Setting Up Eclipse Neon with the Source Code

1. Download and install Eclipse Neon. Get the "Eclipse IDE for Eclipse Committers" package or the Eclipse SDK.
2. In Eclipse, click Help > Install New Software.  From the "Work with" dropdown, select "Neon."  After the list of features loads:
   * Expand "Programming Languages," and check "C/C++ Development Tools SDK."
   * Expand "Programming Languages," and check "C/C++ UPC (Unified Parallel C) Support."
   * Expand "General Purpose Tools," and check "Parallel Tools Platform."
   * Expand "General Purpose Tools," and check "PTP Parallel Language Development Tools UPC Support."
   * Then click Next, Next, Accept, Finish.
3. Click Window > Perspective > Other, select Git, and click OK.  This will open the Git perspective.
4. In the Git Repositories view, click "Clone a Git repository," select "Clone URI," and click Next.  Enter the URI: git@github.com:joverbey/oaccrefac.git then click Next.  Check "Import all existing Eclipse projects after clone finishes," and click Finish.
5. Click Window > Preferences.  Expand "Plug-in Development" and select "API Baselines."  Click "Add Baseline."  Make sure "An existing Eclipse installation directory" is checked, and click Next.  In the "Name" box, enter "Installation."  Click Refresh, then click Finish.  Then, click OK to close the Preferences dialog, and when prompted, choose "Yes" to perform a full build.
