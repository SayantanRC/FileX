
# Build instructions (in Linux terminal):
1. Make sure you have `git` installed on your system. Type `git --version` to check installed version. If not installed, refer to your distribution's package manager to check how to install it.
2. Change to your preferred directory. Here we are assuming your home directory on Linux.
   ```
   cd ~
   ```
3. Clone this repository. Then change directory into the repository.
   ```
   git clone https://github.com/SayantanRC/FileX.git
   cd FileX
   ```
4. You can now directly use this directory in your projects.  
  - To add it as an external library, open your project in Android Studio.  
  - Open `settings.gradle` file.  
  - Add the below lines:  
    ```
    include ':FileX'
    project(':FileX').projectDir=new File('/home/[USERNAME]/FileX')
    ```
    where `[USERNAME]` is your Linux username without square brackets.
  - In your app level `build.gradle` file, add the following in dependencies:   
    ```
    implementation project(path: ':FileX')
    ```
  - Then Gradle sync.  
  - Advantage of the is that you can again cd to the `FileX` cloned repository (`cd ~/FileX`) and pull new changes/commits anytime you wish (`git pull`) without waiting for releases on jitpack or anywhere. 
5. However, if you with to build AAR, use the following commands. Here we are assuming your `android-studio` directory is under home and you are using the Java runtime provided by it.  
   ```
   cd ~/FileX
   export JAVA_HOME="$HOME/android-studio/jre/"
   ./gradlew assembleRelease -xtest -xlint
   ```
   The compiled AAR is located under the FileX directory -> build/outputs/aar/FileX-release.aar
