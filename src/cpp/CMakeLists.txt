file(GLOB SRC_FILES
    "*.cpp"
)

add_library(customer_core ${SRC_FILES})

# allow headers to be included
target_include_directories(customer_core PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

# App executable
add_executable(customerApp main.cpp)
target_link_libraries(customerApp PRIVATE customer_core)
