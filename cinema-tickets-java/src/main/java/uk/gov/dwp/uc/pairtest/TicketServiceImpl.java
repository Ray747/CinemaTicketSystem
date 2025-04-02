package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import java.util.Arrays;
import java.util.List;

public class TicketServiceImpl implements TicketService {
    private static final int MAX_TICKETS = 25;
    private static final int ADULT_PRICE = 25;
    private static final int CHILD_PRICE = 15;
    private static final int INFANT_PRICE = 0;

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(TicketPaymentService paymentService,
                             SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        List<TicketTypeRequest> requests = Arrays.asList(ticketTypeRequests);

        validateAccount(accountId);
        validateTicketRequests(requests);

        int totalAmount = calculateTotalAmount(requests);
        int totalSeats = calculateTotalSeats(requests);

        paymentService.makePayment(accountId, totalAmount);
        reservationService.reserveSeat(accountId, totalSeats);
    }

    private void validateAccount(Long accountId) throws InvalidPurchaseException {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account ID");
        }
    }

    private void validateTicketRequests(List<TicketTypeRequest> requests)
            throws InvalidPurchaseException {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidPurchaseException("No tickets requested");
        }

        int totalTickets = requests.stream()
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();

        if (totalTickets > MAX_TICKETS) {
            throw new InvalidPurchaseException("Maximum 25 tickets allowed");
        }

        boolean hasAdult = requests.stream()
                .anyMatch(req -> req.getTicketType() == TicketTypeRequest.Type.ADULT);

        boolean hasChildOrInfant = requests.stream()
                .anyMatch(req -> req.getTicketType() != TicketTypeRequest.Type.ADULT);

        if (hasChildOrInfant && !hasAdult) {
            throw new InvalidPurchaseException("Child/Infant tickets require an Adult ticket");
        }
    }

    private int calculateTotalAmount(List<TicketTypeRequest> requests) {
        return requests.stream()
                .mapToInt(req -> {
                    switch (req.getTicketType()) {
                        case ADULT:
                            return ADULT_PRICE * req.getNoOfTickets();
                        case CHILD:
                            return CHILD_PRICE * req.getNoOfTickets();
                        case INFANT:
                            return INFANT_PRICE * req.getNoOfTickets();
                        default:
                            return 0;
                    }
                })
                .sum();
    }

    private int calculateTotalSeats(List<TicketTypeRequest> requests) {
        return requests.stream()
                .filter(req -> req.getTicketType() != TicketTypeRequest.Type.INFANT)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();
    }
}