cmake_minimum_required(VERSION 3.14)
project(WorkerManagementSystem)

set(CMAKE_CXX_STANDARD 17)


set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${PROJECT_BINARY_DIR})

# Add nlohmann/json include path (header only)
include_directories(external/nlohmann)

# Add GoogleTest source folder
add_subdirectory(external/googletest)

enable_testing()

# Add project source and tests
add_subdirectory(src/cpp)
add_subdirectory(test/cpp)

