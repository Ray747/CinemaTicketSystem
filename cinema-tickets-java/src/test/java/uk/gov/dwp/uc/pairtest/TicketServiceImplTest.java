package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {
    @Mock private TicketPaymentService paymentService;
    @Mock private SeatReservationService reservationService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Test
    void shouldRejectInvalidAccountId() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(-1L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)));
    }

    @Test
    void shouldRejectEmptyTicketRequest() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L));
    }

    @Test
    void shouldRejectMoreThanMaxTickets() {
        TicketTypeRequest[] requests = new TicketTypeRequest[26];
        for (int i = 0; i < 26; i++) {
            requests[i] = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        }
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, requests));
    }

    @Test
    void shouldRequireAdultForChildTicket() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1)));
    }

    @Test
    void shouldRequireAdultForInfantTicket() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)));
    }

    @Test
    void shouldCalculateCorrectPayment() throws InvalidPurchaseException {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)
        );
        verify(paymentService).makePayment(1L, 2*25 + 3*15);
    }

    @Test
    void shouldCalculateCorrectSeats() throws InvalidPurchaseException {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)
        );
        verify(reservationService).reserveSeat(1L, 5); // Adults + Children (no infants)
    }

    @Test
    void shouldAcceptValidRequest() {
        assertDoesNotThrow(() -> ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1))
        );
    }
}