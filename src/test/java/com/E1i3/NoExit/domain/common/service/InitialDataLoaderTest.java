package com.E1i3.NoExit.domain.common.service;

import com.E1i3.NoExit.domain.grade.domain.Grade;
import com.E1i3.NoExit.domain.grade.repository.GradeRepository;
import com.E1i3.NoExit.domain.member.domain.Member;
import com.E1i3.NoExit.domain.member.repository.MemberRepository;
import com.E1i3.NoExit.domain.owner.domain.Owner;
import com.E1i3.NoExit.domain.owner.repository.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/*
 * 시드 사장님 계정 비밀번호가 passwordEncoder 를 거쳐 저장되는지 고정한다.
 * (application.yml ddl-auto: create 라 기동마다 스키마가 재생성되므로 "기존 평문 행" 보정은 다루지 않는다.)
 */
@ExtendWith(MockitoExtension.class)
class InitialDataLoaderTest {

    @Mock private OwnerRepository ownerRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private GradeRepository gradeRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private InitialDataLoader loader;

    @BeforeEach
    void setUp() {
        loader = new InitialDataLoader();
        ReflectionTestUtils.setField(loader, "ownerRepository", ownerRepository);
        ReflectionTestUtils.setField(loader, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(loader, "gradeRepository", gradeRepository);
        ReflectionTestUtils.setField(loader, "passwordEncoder", passwordEncoder);

        when(gradeRepository.findById(1L)).thenReturn(Optional.of(Grade.builder().id(1L).build()));
        // 회원 시드는 이 테스트의 관심사가 아니므로 이미 존재하는 것으로 둔다
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(Member.builder().build()));
    }

    @Test
    void newSeedOwners_areSavedWithEncodedPassword() throws Exception {
        when(ownerRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        loader.run();

        ArgumentCaptor<Owner> captor = ArgumentCaptor.forClass(Owner.class);
        verify(ownerRepository, times(2)).save(captor.capture());
        List<Owner> saved = captor.getAllValues();

        for (String email : List.of("owner@test.com", "test@test.com")) {
            Owner owner = saved.stream().filter(o -> email.equals(o.getEmail())).findFirst()
                    .orElseThrow(() -> new AssertionError(email + " not seeded"));
            assertNotEquals("12341234", owner.getPassword(), email + " stored in plain text");
            assertTrue(passwordEncoder.matches("12341234", owner.getPassword()), email + " not encoded");
        }
    }

    @Test
    void existingSeedOwners_areNotOverwritten() throws Exception {
        when(ownerRepository.findByEmail(anyString())).thenReturn(Optional.of(Owner.builder().build()));

        loader.run();

        verify(ownerRepository, never()).save(any(Owner.class));
    }
}
