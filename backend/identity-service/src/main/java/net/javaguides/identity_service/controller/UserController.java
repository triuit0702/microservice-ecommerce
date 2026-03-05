package net.javaguides.identity_service.controller;


import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.identity_service.audit.Audit;
import net.javaguides.identity_service.dto.SignUpRequest;
import net.javaguides.identity_service.dto.UserDto;
import net.javaguides.identity_service.exception.AuthException;
import net.javaguides.identity_service.exception.ResourceNotFoundException;
import net.javaguides.identity_service.service.AuthService;
import net.javaguides.identity_service.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;


    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(@RequestParam(defaultValue = "0") int page, // trang số mấy
                                                     @RequestParam(defaultValue = "5") int size)  // số lượng phần tữ trong 1 trang
    {

        Page<UserDto> userList = userService.getAllUsers(page, size);
        return ResponseEntity.ok(userList);
    }

    //@PreAuthorize("hasAuthority('USER_CREATE')")
    @PreAuthorize("@userPolicy.canCreate(authentication)")
    @PostMapping
    public ResponseEntity<ApiResponse<String>> addNewUser(@RequestBody SignUpRequest signUpRequest) {
        try {
            String message = authService.saveUser(signUpRequest);
            return new ResponseEntity<>(ApiResponse.success(message), HttpStatus.CREATED);
        }
        catch(AuthException e){
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        }
        catch(Exception e){
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PutMapping
    public ResponseEntity<ApiResponse<String>> update(@RequestBody SignUpRequest signUpRequest) {
        try {
            userService.upateUser(signUpRequest);
            return new ResponseEntity<>(ApiResponse.success("user updated success"), HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>>  delete(@RequestParam Long userId) {
        try {
            userService.deleteUser(userId);
            return new ResponseEntity<>(ApiResponse.success("user deleted success"), HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
